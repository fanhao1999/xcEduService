package com.xuecheng.learning.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.config.RabbitMQConfig;
import com.xuecheng.learning.service.LearningService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChooseCourseTask {

    @Autowired
    private LearningService learningService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_ADDCHOOSECOURSE)
    public void receiveChoosecourseTask(XcTask xcTask) {
        //取出消息内容
        String requestBody = xcTask.getRequestBody();
        Map map = JSON.parseObject(requestBody, Map.class);
        String userId = (String) map.get("userId");
        String courseId = (String) map.get("courseId");
        // TODO 解析valid, startTime, endTime...
        //添加选课
        ResponseResult responseResult = learningService.addcourse(userId, courseId, null,
                null, null, xcTask);
        if (responseResult.isSuccess()) {
            rabbitTemplate.convertAndSend(xcTask.getMqExchange(),
                    RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE_KEY, xcTask);
        }
    }
}
