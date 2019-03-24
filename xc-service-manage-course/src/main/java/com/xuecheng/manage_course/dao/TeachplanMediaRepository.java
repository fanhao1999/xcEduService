package com.xuecheng.manage_course.dao;

import com.xuecheng.framework.domain.course.TeachplanMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeachplanMediaRepository extends JpaRepository<TeachplanMedia, String> {

    //从TeachplanMedia查询课程计划媒资信息
    List<TeachplanMedia> findByCourseId(String courseId);
}
