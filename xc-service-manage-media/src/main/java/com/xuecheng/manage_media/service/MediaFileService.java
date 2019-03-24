package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MediaFileService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    //文件列表分页查询
    public QueryResponseResult<MediaFile> findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 10 : size;

        // 非空判断
        queryMediaFileRequest = queryMediaFileRequest == null ? new QueryMediaFileRequest() : queryMediaFileRequest;

        //查询条件匹配器
        //tag字段模糊匹配
        //文件原始名称模糊匹配
        //处理状态精确匹配（默认）
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("tag", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("fileOriginalName", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("processStatus", ExampleMatcher.GenericPropertyMatchers.exact());
        //查询条件对象
        MediaFile mediaFile = new MediaFile();
        if (StringUtils.isNotBlank(queryMediaFileRequest.getTag())) {
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if (StringUtils.isNotBlank(queryMediaFileRequest.getFileOriginalName())) {
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if (StringUtils.isNotBlank(queryMediaFileRequest.getProcessStatus())) {
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        //定义example实例
        Example<MediaFile> example = Example.of(mediaFile, exampleMatcher);
        Pageable pageable = PageRequest.of(page - 1, size);
        //分页查询
        Page<MediaFile> mediaFilePage = mediaFileRepository.findAll(example, pageable);

        QueryResult<MediaFile> mediaFileQueryResult = new QueryResult<>();
        mediaFileQueryResult.setList(mediaFilePage.getContent());
        mediaFileQueryResult.setTotal(mediaFilePage.getTotalElements());
        return new QueryResponseResult<>(CommonCode.SUCCESS, mediaFileQueryResult);
    }
}
