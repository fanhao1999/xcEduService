package com.xuecheng.manage_media.controller;

import com.xuecheng.api.media.MediaUploadControllerApi;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.service.MediaUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media/upload")
public class MediaUploadController implements MediaUploadControllerApi {

    @Autowired
    private MediaUploadService mediaUploadService;

    @Override
    @PostMapping("/register")
    public ResponseResult register(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("mimetype") String mimetype,
            @RequestParam("fileExt") String fileExt
    ) {
        return mediaUploadService.register(fileMd5, fileName, fileSize, mimetype, fileExt);
    }

    @Override
    @PostMapping("/checkchunk")
    public CheckChunkResult checkchunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunk") Integer chunk,
            @RequestParam("chunkSize") Integer chunkSize
    ) {
        return mediaUploadService.checkchunk(fileMd5, chunk, chunkSize);
    }

    @Override
    @PostMapping("/uploadchunk")
    public ResponseResult uploadchunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunk") Integer chunk,
            @RequestParam("fileMd5") String fileMd5
    ) {
        return mediaUploadService.uploadchunk(file, chunk, fileMd5);
    }

    @Override
    @PostMapping("/mergechunks")
    public ResponseResult mergechunks(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("mimetype") String mimetype,
            @RequestParam("fileExt") String fileExt
    ) {
        return mediaUploadService.mergechunks(fileMd5, fileName, fileSize, mimetype, fileExt);
    }
}
