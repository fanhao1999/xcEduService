package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.filesystem.config.FastdfsProperties;
import com.xuecheng.filesystem.config.UploadProperties;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;

@Service
@Transactional
@EnableConfigurationProperties({FastdfsProperties.class, UploadProperties.class})
public class FileSystemService {

    @Autowired
    private FileSystemRepository fileSystemRepository;

    @Autowired
    private FastdfsProperties fastdfsProperties;

    @Autowired
    private UploadProperties uploadProperties;

    public UploadFileResult upload(MultipartFile multipartFile, String filetag, String businesskey, String metadata) {
        if (multipartFile == null) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        // 校验文件是否为图片
        checkImageFile(multipartFile);
        //上传文件到fdfs
        String fileId = fdfsUpload(multipartFile);
        if (StringUtils.isBlank(fileId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
        }
        //创建文件信息对象
        FileSystem fileSystem = new FileSystem();
        //文件id
        fileSystem.setFileId(fileId);
        //文件在文件系统中的路径
        fileSystem.setFilePath(fileId);
        fileSystem.setFileSize(multipartFile.getSize());
        fileSystem.setFileName(multipartFile.getOriginalFilename());
        fileSystem.setFileType(multipartFile.getContentType());
        fileSystem.setBusinesskey(businesskey);
        fileSystem.setFiletag(filetag);
        if (StringUtils.isNotBlank(metadata)) {
            try {
                Map map = JSON.parseObject(metadata, Map.class);
                fileSystem.setMetadata(map);
            } catch (Exception e) {
                ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_METAERROR);
            }
        }
        FileSystem save = fileSystemRepository.save(fileSystem);
        if (save == null) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
        }
        return new UploadFileResult(CommonCode.SUCCESS, save);
    }

    // 校验文件是否为图片
    private void checkImageFile(MultipartFile multipartFile) {
        try {
            String contentType = multipartFile.getContentType();
            if (!uploadProperties.getAllowTypes().contains(contentType)) {
                ExceptionCast.cast(FileSystemCode.FS_INVALID_FILE_TYPE);
            }
            BufferedImage image = ImageIO.read(multipartFile.getInputStream());
            if (image == null) {
                ExceptionCast.cast(FileSystemCode.FS_INVALID_FILE_TYPE);
            }
        } catch (Exception e) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
        }
    }

    // 上传文件到fdfs，返回文件id
    public String fdfsUpload(MultipartFile multipartFile) {
        try {
            //加载fdfs的配置
            initFdfsConfig();
            //创建tracker client
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取storage
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            //创建storage client
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            //上传文件
            //文件字节
            byte[] bytes = multipartFile.getBytes();
            //文件扩展名
            String extName = StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".");
            //文件id
            String fileId = storageClient1.upload_file1(bytes, extName, null);
            return fileId;
        } catch (Exception e) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
            return null;
        }
    }

    //加载fdfs的配置
    private void initFdfsConfig() {
        try {
            ClientGlobal.initByTrackers(fastdfsProperties.getTracker_servers());
            ClientGlobal.setG_charset(fastdfsProperties.getCharset());
            ClientGlobal.setG_connect_timeout(fastdfsProperties.getConnect_timeout_in_seconds());
            ClientGlobal.setG_network_timeout(fastdfsProperties.getNetwork_timeout_in_seconds());
        } catch (Exception e) {
            //初始化文件系统出错
            ExceptionCast.cast(FileSystemCode.FS_INITFDFSERROR);
        }
    }
}
