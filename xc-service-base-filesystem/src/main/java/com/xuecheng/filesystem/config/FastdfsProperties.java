package com.xuecheng.filesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "xuecheng.fastdfs")
public class FastdfsProperties {

    private int connect_timeout_in_seconds;

    private int network_timeout_in_seconds;

    private String charset;

    private String tracker_servers;
}
