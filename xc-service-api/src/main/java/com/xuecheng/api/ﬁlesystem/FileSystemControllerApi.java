package com.xuecheng.api.ﬁlesystem;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value="文件管理接口", description = "文件管理接口，提供文件的增、删、改、查")
public interface FileSystemControllerApi {

    /**
     * 上传文件
     * @param multipartFile
     * @param filetag
     * @param businesskey
     * @param metadata
     * @return
     */
    @ApiOperation("上传文件")
    UploadFileResult upload(
            MultipartFile multipartFile,
            String filetag,
            String businesskey,
            String metadata
    );
}
