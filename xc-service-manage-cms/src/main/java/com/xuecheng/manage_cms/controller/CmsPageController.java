package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsPageControllerApi;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.service.CmsPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cms/page")
public class CmsPageController implements CmsPageControllerApi {

    @Autowired
    private CmsPageService cmsPageService;

    /**
     * 根据站点id、模板id以及页面别名(模糊查询)来进行分页查询
     * @param page
     * @param size
     * @param queryPageRequest
     * @return
     */
    @Override
    @GetMapping("/list/{page}/{size}")
    public QueryResponseResult findList(
            @PathVariable("page") int page,
            @PathVariable("size") int size,
            QueryPageRequest queryPageRequest
    ) {
        return cmsPageService.findList(page, size, queryPageRequest);
    }

    /**
     * 新增页面
     * @param cmsPage
     * @return
     */
    @Override
    @PostMapping("/add")
    public CmsPageResult addPage(@RequestBody CmsPage cmsPage) {
        return cmsPageService.addPage(cmsPage);
    }

    /**
     * 根据Id查询页面
     * @param pageId
     * @return
     */
    @Override
    @GetMapping("/get/{id}")
    public CmsPage findById(@PathVariable("id") String pageId) {
        return cmsPageService.findById(pageId);
    }

    /**
     * 根据Id更新页面
     * @param pageId
     * @param cmsPage
     * @return
     */
    @Override
    @PutMapping("/edit/{id}")
    public CmsPageResult updatePage(@PathVariable("id") String pageId, @RequestBody CmsPage cmsPage) {
        return cmsPageService.updatePage(pageId, cmsPage);
    }

    /**
     * 根据Id删除页面
     * @param pageId
     * @return
     */
    @Override
    @DeleteMapping("/del/{id}")
    public ResponseResult deletePage(@PathVariable("id") String pageId) {
        return cmsPageService.deletePage(pageId);
    }

    /**
     * 发布页面
     * @param pageId
     * @return
     */
    @Override
    @PostMapping("/postPage/{id}")
    public ResponseResult postPage(@PathVariable("id") String pageId) {
        return cmsPageService.postPage(pageId);
    }

    /**
     * 保存页面
     * @param cmsPage
     * @return
     */
    @Override
    @PostMapping("/save")
    public CmsPageResult save(@RequestBody CmsPage cmsPage) {
        return cmsPageService.save(cmsPage);
    }

    /**
     * 一键发布页面
     * @param cmsPage
     * @return
     */
    @Override
    @PostMapping("/postPageQuick")
    public CmsPostPageResult postPageQuick(@RequestBody CmsPage cmsPage) {
        return cmsPageService.postPageQuick(cmsPage);
    }
}
