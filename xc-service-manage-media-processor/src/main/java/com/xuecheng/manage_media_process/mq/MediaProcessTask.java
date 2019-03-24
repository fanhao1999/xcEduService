package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    //上传文件根目录
    @Value("${xc-service-manage-media.video-location}")
    private String videoLocation;

    //ffmpeg绝对路径
    @Value("${xc-service-manage-media.ffmpeg-path}")
    private String ffmpegPath;

    //视频处理方法
    @RabbitListener(
            queues = "${xc-service-manage-media.mq.queue-media-video-processor}",
            containerFactory = "customContainerFactory"
    )
    public void receiveMediaProcessTask(String msg) {
        Map msgMap = JSON.parseObject(msg, Map.class);
        //解析消息
        //媒资文件id
        String mediaId = (String) msgMap.get("mediaId");
        //获取媒资文件信息
        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(mediaId);
        if (!mediaFileOptional.isPresent()) {
            return;
        }
        MediaFile mediaFile = mediaFileOptional.get();
        //媒资文件类型
        String fileType = mediaFile.getFileType();
        //目前只处理avi文件
        if (!StringUtils.equals(fileType, "avi")) {
            //处理状态为无需处理
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        } else {
            //处理状态为未处理
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        //生成mp4
        String videoPath = videoLocation + mediaFile.getFilePath() + mediaFile.getFileName();
        String mp4Name = mediaFile.getFileId() + ".mp4";
        String mp4FolderPath = videoLocation + mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegPath, videoPath, mp4Name, mp4FolderPath);
        String result = mp4VideoUtil.generateMp4();
        if (!StringUtils.equals(result, "success")) {
            //操作失败写入处理日志
            //处理状态为处理失败
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //生成m3u8...
        //此地址为mp4的地址
        String mp4Path = mp4FolderPath + mp4Name;
        String m3u8Name = mediaFile.getFileId() + ".m3u8";
        String m3u8FolderPath = videoLocation + mediaFile.getFilePath() + "hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpegPath, mp4Path, m3u8Name, m3u8FolderPath);
        String tsResult = hlsVideoUtil.generateM3u8();
        if (!StringUtils.equals(tsResult, "success")) {
            //操作失败写入处理日志
            //处理状态为处理失败
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //获取m3u8列表
        List<String> tsList = hlsVideoUtil.get_ts_list();
        //更新处理状态为成功
        //处理状态为处理成功
        mediaFile.setProcessStatus("303002");
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(tsList);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //m3u8文件url
        mediaFile.setFileUrl(mediaFile.getFilePath() + "hls/" + m3u8Name);
        mediaFileRepository.save(mediaFile);
    }
}
