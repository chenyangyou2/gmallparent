package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * author:atGuiGu-mqx
 * date:2022/6/29 11:27
 * 描述：
 **/
@Configuration
public class DelayedMqConfig {

    //  声明一些变量
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    //  基于延迟插件使用 CustomExchange 这个交换机
    @Bean
    public CustomExchange customExchange(){
        //  设置一个参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type","direct");
        //  返回交换机 type：是固定值
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    //  声明队列
    @Bean
    public Queue delayQueue(){
        //  默认返回
        return new Queue(queue_delay_1,true,false,false);
    }

    //  声明绑定关系
    @Bean
    public Binding delayBinding(){
        //  noargs() 用它是跟交换机类型有关系。
        return BindingBuilder.bind(delayQueue()).to(customExchange()).with(routing_delay).noargs();
    }

}