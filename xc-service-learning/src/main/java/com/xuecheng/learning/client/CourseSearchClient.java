package com.xuecheng.learning.client;

import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient("xc-search-service")
public interface CourseSearchClient {

    /**
     * 根据课程计划查询媒资信息
     * @param teachplanId
     * @return
     */
    @GetMapping("/search/course/getmedia/{teachplanId}")
    TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId);
}
