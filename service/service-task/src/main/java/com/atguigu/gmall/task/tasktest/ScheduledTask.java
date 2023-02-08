package com.atguigu.gmall.task.tasktest;

import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.constant.MqConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    // 设置定时任务的规则 每隔10秒执行一次
    //  分，时，日，月，周，年
    //  {秒数} {分钟} {小时} {日期} {月份} {星期} {年份可以为空}
    //  0-59   0-59  0-23   1-31  1-12   1-7
    @Scheduled(cron = "0/10 * * * * ?")
    public void testImportToRedis(){
        //  发送消息
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"ok");
    }
}
