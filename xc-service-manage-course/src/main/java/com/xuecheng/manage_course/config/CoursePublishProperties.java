package com.xuecheng.manage_course.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "course-publish")
public class CoursePublishProperties {

    private String siteId;
    private String pageWebPath;
    private String pagePhysicalPath;
    private String templateId;
    private String dataUrl;
    private String previewUrl;
}
