package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class DeadLetterMqConfig {

    // 声明一些变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //定义交换机
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange(exchange_dead , true , false ,null);
    }
    @Bean
    public Queue queue1(){
        // 设置消息的过期时间
        HashMap<String, Object> map = new HashMap<>();
        // 10秒
        map.put("x-message-ttl" , 10000);
        map.put("x-dead-letter-exchange",exchange_dead);
        map.put("x-dead-letter-routing-key",routing_dead_2);
        return new Queue(queue_dead_1 , true , false , false ,map);
    }
    //设置队列1的绑定关系
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }
    //设置队列2
    @Bean
    public Queue queue2() {
        return new Queue(queue_dead_2 ,true , false , false , null);
    }
    //设置队列的绑定关系
    @Bean
    public Binding binding2(){
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }


















}

