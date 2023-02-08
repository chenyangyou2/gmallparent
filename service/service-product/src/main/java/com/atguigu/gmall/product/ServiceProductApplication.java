package com.atguigu.gmall.product;

import com.atguigu.gmall.common.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceProductApplication implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;

    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        RBloomFilter<Object> rBloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        //初始化数据规模 设定一个误判率
        // 相当于10万件 SKU 自动计算有多少个hash函数 byte 数组的长度
        rBloomFilter.tryInit(100000  ,  0.01);
    }
}
