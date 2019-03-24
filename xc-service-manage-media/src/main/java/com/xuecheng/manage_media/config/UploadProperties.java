package com.xuecheng.manage_media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "xuecheng.upload")
public class UploadProperties {

    private List<String> allowTypes;
}
