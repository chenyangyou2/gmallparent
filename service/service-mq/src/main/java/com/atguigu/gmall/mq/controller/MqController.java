package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import net.bytebuddy.asm.Advice;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * http://localhost:8282/mq/sendConfirm
     *发送消息
     */
    @GetMapping("sendConfirm")
    public Result sendConfirm() {

        //调用发送消息
        rabbitService.sendMsg("exchange.confirm666", "routing.confirm", "来单了！！！！！！！！！");

        return Result.ok();
    }

    //发送延迟消息
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送时间" + simpleDateFormat.format(new Date()));
        //发送消息
        rabbitService.sendMsg(DeadLetterMqConfig.exchange_dead , DeadLetterMqConfig.routing_dead_1 , "ok");
        return Result.ok();
    }

    //基于延迟插件的延迟消息
    @GetMapping("sendDelay")
    public Result sendDelay() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送时间" + simpleDateFormat.format(new Date()));
//        this.rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay , "iuok-imok" , (message) -> {
//            message.getMessageProperties().setDelay(10000);
//            return message;
//        });
        rabbitService.sendDelayMsg(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay , "iuok" , 3);
        return Result.ok();
    }
}