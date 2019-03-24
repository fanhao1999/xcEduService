package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value="cms页面管理接口", description = "cms页面管理接口，提供页面的增、删、改、查")
public interface CmsPageControllerApi {

    /**
     * 根据站点id、模板id以及页面别名(模糊查询)来进行分页查询
     * @param page
     * @param size
     * @param queryPageRequest
     * @return
     */
    @ApiOperation("分页查询页面列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name="page", value = "页码", required = true, paramType = "path", dataType = "int"),
            @ApiImplicitParam(name="size", value = "每页记录数", required = true, paramType = "path", dataType = "int")
    })
    QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest);

    /**
     * 新增页面
     * @param cmsPage
     * @return
     */
    @ApiOperation("新增页面")
    CmsPageResult addPage(CmsPage cmsPage);

    /**
     * 根据Id查询页面
     * @param pageId
     * @return
     */
    @ApiOperation("根据Id查询页面")
    CmsPage findById(String pageId);

    /**
     * 根据Id更新页面
     * @param pageId
     * @param cmsPage
     * @return
     */
    @ApiOperation("根据Id更新页面")
    CmsPageResult updatePage(String pageId, CmsPage cmsPage);

    /**
     * 根据Id删除页面
     * @param pageId
     * @return
     */
    @ApiOperation("根据Id删除页面")
    ResponseResult deletePage(String pageId);

    /**
     * 发布页面
     * @param pageId
     * @return
     */
    @ApiOperation("发布页面")
    ResponseResult postPage(String pageId);

    /**
     * 保存页面
     * @param cmsPage
     * @return
     */
    @ApiOperation("保存页面")
    CmsPageResult save(CmsPage cmsPage);

    /**
     * 一键发布页面
     * @param cmsPage
     * @return
     */
    @ApiOperation("一键发布页面")
    CmsPostPageResult postPageQuick(CmsPage cmsPage);
}
