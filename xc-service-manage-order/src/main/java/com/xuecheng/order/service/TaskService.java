package com.xuecheng.order.service;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskService {

    @Autowired
    private XcTaskRepository xcTaskRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private XcTaskHisRepository xcTaskHisRepository;

    //取出前n条任务,取出指定时间之前处理的任务
    public List<XcTask> findTaskList(int page, int size, Date updateTime) {
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 10 :size;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<XcTask> xcTaskPage = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);
        List<XcTask> xcTaskList = xcTaskPage.getContent();
        return xcTaskList;
    }

    //发送消息
    public void publish(XcTask xcTask, String exchange, String routingkey) {
        //查询任务
        Optional<XcTask> xcTaskOptional = xcTaskRepository.findById(xcTask.getId());
        if (xcTaskOptional.isPresent()) {
            xcTask = xcTaskOptional.get();
            rabbitTemplate.convertAndSend(exchange, routingkey, xcTask);
            //更新任务时间为当前时间
            xcTask.setUpdateTime(new Date());
            xcTaskRepository.save(xcTask);
        }
    }

    //使用乐观锁再次获取任务
    public int getTask(String taskId, int version) {
        return xcTaskRepository.updateTaskVersion(taskId, version);
    }

    //删除任务
    public void finishTask(String taskId) {
        Optional<XcTask> xcTaskOptional = xcTaskRepository.findById(taskId);
        if (xcTaskOptional.isPresent()) {
            XcTask xcTask = xcTaskOptional.get();
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask, xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }
}
