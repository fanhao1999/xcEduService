package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.manage_cms_client.service.PageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ConsumerPostPage {

    @Autowired
    private PageService pageService;

    @RabbitListener(queues = "${xuecheng.mq.queue}")
    public void postPage(String msg) {
        // 解析消息
        Map map = JSON.parseObject(msg, Map.class);
        // 得到消息中的页面id
        String pageId = (String) map.get("pageId");
        // 下载该页面, 保存到服务器物理路径
        pageService.savePageToServerPath(pageId);
    }
}
