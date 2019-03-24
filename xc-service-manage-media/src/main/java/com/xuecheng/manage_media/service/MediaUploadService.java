package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.config.UploadProperties;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Slf4j
@Service
@Transactional
@EnableConfigurationProperties(UploadProperties.class)
public class MediaUploadService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Value("${xc-service-manage-media.upload-location}")
    private String uploadPath;

    @Autowired
    private UploadProperties uploadProperties;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //视频处理路由
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    public String routingkey_media_video;

    //得到文件所在目录
    private String getFileFolderPath(String fileMd5) {
        return uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/";
    }

    //得到文件路径
    private String getFilePath(String fileMd5, String fileExt) {
        return uploadPath + fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/"
                + fileMd5 + "." + fileExt;
    }

    //创建文件目录
    private boolean createFileFold(String fileMd5) {
        String fileFolderPath = getFileFolderPath(fileMd5);
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            //创建文件夹
            boolean mkdirs = fileFolder.mkdirs();
            return mkdirs;
        }
        return true;
    }

    //得到块文件所在目录
    private String getChunkFileFolderPath(String fileMd5) {
        return getFileFolderPath(fileMd5) + "chunks/";
    }

    //创建块文件目录
    private boolean createChunkFileFolder(String fileMd5) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()) {
            //创建文件夹
            boolean mkdirs = chunkFileFolder.mkdirs();
            return  mkdirs;
        }
        return true;
    }

    //得到文件目录相对路径，路径中去掉根目录
    private String getFileFolderRelativePath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/";
    }

    //文件上传注册
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        // 检查文件类型是否合法
        if (!uploadProperties.getAllowTypes().contains(mimetype)) {
            ExceptionCast.cast(MediaCode.UPLOAD_INVALID_FILE_TYPE);
        }
        //检查文件是否上传
        //1、得到文件的路径
        String filePath = getFilePath(fileMd5, fileExt);
        File file = new File(filePath);
        //2、查询数据库文件是否存在
        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(fileMd5);
        //文件存在直接返回
        if (file.exists() && mediaFileOptional.isPresent()) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        boolean fileFold = createFileFold(fileMd5);
        if (!fileFold) {
            //上传文件目录创建失败
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //检查块文件
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //得到块文件所在路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //块文件的文件名称以1,2,3..序号命名，没有扩展名
        File chunkFile = new File(chunkFileFolderPath + chunk);
        if (chunkFile.exists()) {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, true);
        }
        return new CheckChunkResult(CommonCode.SUCCESS, false);
    }

    //块文件上传
    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        if (file == null) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        //创建块文件目录
        boolean chunkFileFolder = createChunkFileFolder(fileMd5);
        if (!chunkFileFolder) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }
        //块文件
        File chunkfile = new File(getChunkFileFolderPath(fileMd5) + chunk);
        //上传的块文件
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = file.getInputStream();
            outputStream = new FileOutputStream(chunkfile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("upload chunk file fail:{}", e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //获取块文件的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        //合并文件路径
        String mergeFilePath = getFilePath(fileMd5, fileExt);
        //创建合并文件
        File mergeFile = new File(mergeFilePath);
        //合并文件存在先删除再创建
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        boolean newFile = false;
        try {
            newFile = mergeFile.createNewFile();
        } catch (IOException e) {
            log.error("mergechunks..create mergeFile fail:{}", e.getMessage());
        }
        if (!newFile) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //获取块文件，此列表是已经排好序的列表
        List<File> chunkFileList = getChunkFiles(chunkFileFolder);
        //合并文件
        mergeFile = mergeFile(mergeFile, chunkFileList);
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        // 合并成功后删除分块文件夹
        try {
            FileUtils.deleteDirectory(chunkFileFolder);
        } catch (Exception e) {
            log.error("删除分块文件夹chunks失败, 失败原因:{}", e.getMessage());
        }
        //校验文件
        boolean checkResult = checkFileMd5(mergeFile, fileMd5);
        if (!checkResult) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //将文件信息保存到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5 + "." + fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderRelativePath(fileMd5));
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        MediaFile save = mediaFileRepository.save(mediaFile);
        if (save == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //向MQ发送视频处理消息
        sendProcessVideoMsg(save.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取所有块文件
    private List<File> getChunkFiles(File chunkFileFolder) {
        //获取路径下的所有块文件
        File[] chunkFiles = chunkFileFolder.listFiles();
        //将文件数组转成list，并排序
        List<File> chunkFileList = Arrays.asList(chunkFiles);
        //排序
        Collections.sort(chunkFileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName())) {
                    return 1;
                }
                return -1;
            }
        });
        return chunkFileList;
    }

    //合并文件
    private File mergeFile(File mergeFile, List<File> chunkFileList) {
        try {
            //创建写文件对象
            RandomAccessFile rafWrite = new RandomAccessFile(mergeFile, "rw");
            //遍历分块文件开始合并
            //读取文件缓冲区
            byte[] b = new byte[1024];
            for (File chunkFile : chunkFileList) {
                RandomAccessFile rafRead = new RandomAccessFile(chunkFile, "r");
                int len = -1;
                //读取分块文件
                while ((len = rafRead.read(b)) != -1) {
                    //向合并文件中写数据
                    rafWrite.write(b, 0, len);
                }
                rafRead.close();
            }
            rafWrite.close();
        } catch (Exception e) {
            log.error("merge file error:{}", e.getMessage());
            return null;
        }
        return mergeFile;
    }

    //校验文件的md5值
    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        if (mergeFile == null || StringUtils.isBlank(fileMd5)) {
            return false;
        }
        //进行md5校验
        InputStream mergeFileInputstream = null;
        try {
            mergeFileInputstream = new FileInputStream(mergeFile);
            //得到文件的md5
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileInputstream);
            //比较md5
            if (fileMd5.equals(mergeFileMd5)) {
                return true;
            }
        } catch (Exception e) {
            log.error("checkFileMd5 error, file is:{}, md5 is:{}", mergeFile.getAbsoluteFile(), fileMd5);
        } finally {
            try {
                mergeFileInputstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //向MQ发送视频处理消息
    public ResponseResult sendProcessVideoMsg(String mediaId) {
        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(mediaId);
        if (!mediaFileOptional.isPresent()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        MediaFile mediaFile = mediaFileOptional.get();
        //发送视频处理消息
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("mediaId", mediaId);
        //发送的消息
        String msg = JSON.toJSONString(msgMap);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK, routingkey_media_video, msg);
        } catch (Exception e) {
            log.error("send media process task error, msg is:{}, error:{}", msg, e.getMessage());
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
