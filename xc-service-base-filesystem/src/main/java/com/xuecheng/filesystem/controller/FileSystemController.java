package com.xuecheng.filesystem.controller;

import com.xuecheng.api.ﬁlesystem.FileSystemControllerApi;
import com.xuecheng.filesystem.service.FileSystemService;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/filesystem")
public class FileSystemController implements FileSystemControllerApi {

    @Autowired
    private FileSystemService fileSystemService;

    /**
     * 上传文件
     * @param multipartFile
     * @param filetag
     * @param businesskey
     * @param metadata
     * @return
     */
    @Override
    @PostMapping("/upload")
    public UploadFileResult upload(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam(value = "filetag", required = false) String filetag,
            @RequestParam(value = "businesskey", required = false) String businesskey,
            @RequestParam(value = "metadata", required = false) String metadata
    ) {
        return fileSystemService.upload(multipartFile, filetag, businesskey, metadata);
    }
}
