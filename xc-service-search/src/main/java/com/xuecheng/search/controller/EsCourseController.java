package com.xuecheng.search.controller;

import com.xuecheng.api.search.EsCourseControllerApi;
import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.search.service.EsCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search/course")
public class EsCourseController implements EsCourseControllerApi {

    @Autowired
    private EsCourseService esCourseService;

    @Override
    @GetMapping("/list/{page}/{size}")
    public QueryResponseResult<CoursePub> list(
            @PathVariable("page") int page,
            @PathVariable("size") int size,
            CourseSearchParam courseSearchParam
    ) {
        return esCourseService.list(page, size, courseSearchParam);
    }

    /**
     * 根据id查询课程信息
     * @param id
     * @return
     */
    @Override
    @GetMapping("/getall/{id}")
    public Map<String, CoursePub> getall(@PathVariable("id") String id) {
        return esCourseService.getall(id);
    }

    /**
     * 根据课程计划查询媒资信息
     * @param teachplanId
     * @return
     */
    @Override
    @GetMapping("/getmedia/{teachplanId}")
    public TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId) {
        //将课程计划id放在数组中，为调用service作准备
        String[] teachplanIds = new String[]{teachplanId};
        //通过service查询ES获取课程媒资信息
        List<TeachplanMediaPub> teachplanMediaPubList = esCourseService.getmedia(teachplanIds);
        if (CollectionUtils.isEmpty(teachplanMediaPubList)) {
            return null;
        }
        return teachplanMediaPubList.get(0);
    }
}
