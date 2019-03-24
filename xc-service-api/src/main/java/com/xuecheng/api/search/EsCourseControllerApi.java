package com.xuecheng.api.search;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.QueryResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

@Api(value = "课程搜索", description = "课程搜索", tags = {"课程搜索"})
public interface EsCourseControllerApi {

    /**
     * 课程综合搜素
     * @param courseSearchParam
     * @return
     */
    @ApiOperation("课程综合搜素")
    QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam);

    /**
     * 根据id查询课程信息
     * @param id
     * @return
     */
    @ApiOperation("根据id查询课程信息")
    Map<String, CoursePub> getall(String id);

    /**
     * 根据课程计划查询媒资信息
     * @param teachplanId
     * @return
     */
    @ApiOperation("根据课程计划查询媒资信息")
    TeachplanMediaPub getmedia(String teachplanId);
}
