package com.xuecheng.api.media;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.QueryResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "媒体文件管理", description = "媒体文件管理接口", tags = {"媒体文件管理接口"})
public interface MediaFileControllerApi {

    /**
     * 查询文件列表
     * @param page
     * @param size
     * @param queryMediaFileRequest
     * @return
     */
    @ApiOperation("查询文件列表")
    QueryResponseResult<MediaFile> findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest);
}
