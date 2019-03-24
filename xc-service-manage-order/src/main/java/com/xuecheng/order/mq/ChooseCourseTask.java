package com.xuecheng.order.mq;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Slf4j
@Component
public class ChooseCourseTask {

    @Autowired
    private TaskService taskService;

    //每隔1分钟扫描消息表，向mq发送消息
    @Scheduled(cron = "0/5 * * * * *")
    public void sendChoosecourseTask() {
        //取出当前时间1分钟之前的时间
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.MINUTE, -1);
        Date time = calendar.getTime();
        List<XcTask> taskList = taskService.findTaskList(1, 10, time);
        //遍历任务列表
        for (XcTask xcTask : taskList) {
            //消息id
            String taskId = xcTask.getId();
            //消息版本号
            Integer version = xcTask.getVersion();
            //调用乐观锁方法校验任务是否可以执行
            if (taskService.getTask(taskId, version) > 0) {
                //发送选课消息
                taskService.publish(xcTask, xcTask.getMqExchange(), xcTask.getMqRoutingkey());
                log.info("send choose course task id:{}", taskId);
            }
        }
    }

    //接收选课响应结果
    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE)
    public void receiveFinishChoosecourseTask(XcTask xcTask) {
        if (xcTask != null && StringUtils.isNotBlank(xcTask.getId())) {
            taskService.finishTask(xcTask.getId());
        }
    }
}
