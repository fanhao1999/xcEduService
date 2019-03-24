package com.xuecheng.manage_course.client;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@FeignClient("xc-service-manage-cms")
public interface CmsPageClient {

    @PostMapping("/cms/page/save")
    CmsPageResult save(@RequestBody CmsPage cmsPage);

    @PostMapping("/cms/page/postPageQuick")
    CmsPostPageResult postPageQuick(@RequestBody CmsPage cmsPage);
}
