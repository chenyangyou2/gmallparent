package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.config.RedissonConfig;
import com.atguigu.gmall.product.service.TestService;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
//    private RedisTemplate redisTemplate;
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public void testLock() {

        //获取锁
        RLock lock = redissonClient.getLock("lock");
        //上锁
//        lock.lock();
        //设置锁的过期时间，过期时间单位
//        lock.lock(10 , TimeUnit.SECONDS);
        boolean res = false;
        try {
            res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res){
                try {
                    //result = true 表示获取到锁，执行业务逻辑
                    String num = redisTemplate.opsForValue().get("num");
                    //判断当前字符串
                    if (StringUtils.isEmpty(num)){
                        return;
                    }
                    //+1 操作
                    int numValue = Integer.parseInt(num);
                    redisTemplate.opsForValue().set("num" , String.valueOf(++numValue));
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String writeLock() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        rwlock.writeLock().lock(10, TimeUnit.SECONDS);
        String s = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("msg" , s);
        return s;
    }

    @Override
    public String readLock() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        rwlock.writeLock().lock(10, TimeUnit.SECONDS);
        String msg = redisTemplate.opsForValue().get("msg");
        return msg;
    }
}

//    @Override
//    public void testLock() {
//
//            //1.setnx key value
//            //Boolean result = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu");
//        //2. set lock atguigu ex 10 nx 锁的过期时间 = 与业务执行的时间想匹配
////        long starTime = System.currentTimeMillis();
//
////        Boolean result = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu", 3, TimeUnit.SECONDS);
//        // 3.使用uuid 作为键的值 申明一个uuid 作为键对应的值
//        String uuid = UUID.randomUUID().toString();
//        Boolean result = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
//        if (result){
//                //result = true 表示获取到锁，执行业务逻辑
//                String num = redisTemplate.opsForValue().get("num");
//
//                //判断当前字符串
//                if (StringUtils.isEmpty(num)){
//                    return;
//                }
//                //+1 操作
//                int numValue = Integer.parseInt(num);
//                redisTemplate.opsForValue().set("num" , String.valueOf(++numValue));
//                //expire 命令在哪执行
//            //删除锁 + 判断 当前工作内存中的uuid 与 缓存中的uuid 是否相等 等于就删除 ， 负责就不删除
////                if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
////                    //解锁
////                    redisTemplate.delete("lock");
////                }
//            String scriptTest ="if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
//                    "then\n" +
//                    "    return redis.call(\"del\",KEYS[1])\n" +
//                    "else\n" +
//                    "    return 0\n" +
//                    "end";
//
//            DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
//            defaultRedisScript.setScriptText(scriptTest);
//            defaultRedisScript.setResultType(Long.class);
//            redisTemplate.execute(defaultRedisScript , Arrays.asList("lock"),uuid);
//
//            }else {
//                // 没获取到锁
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                testLock();
//            }
//
//        // 业务逻辑：endTime - starTime 值
////        long endTime = System.currentTimeMillis();
////
////        System.out.println(starTime - endTime);
//        }
//    }





    //删锁
//    @Override
//    public void testLock() {
//        try {
//            //1.setnx key value
//            Boolean result = redisTemplate.opsForValue().setIfAbsent("lock", "atguigu");
//            if (result){
//                //result = true 表示获取到锁，执行业务逻辑
//                String num = redisTemplate.opsForValue().get("num");
//
//                //判断当前字符串
//                if (StringUtils.isEmpty(num)){
//                    return;
//                }
//                //+1 操作
//                int numValue = Integer.parseInt(num);
//                redisTemplate.opsForValue().set("num" , String.valueOf(++numValue));
//
//            }else {
//                // 没获取到锁
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                testLock();
//            }
//        } finally {
//            //解锁
//            redisTemplate.delete("lock");
//        }
//    }


//    @Override
//    public synchronized void testLock() {
//        /**
//         * 1。在缓存中设置一个num 的key 并且初始化为0  set num 0
//         * 2. 获取到num 对应的数值
//         *      如果num 不为空 ， 则对其进行+1操作 ， 并将这个结果写入缓存中
//         *      如果num 为空，直接返回停止运行
//         */
////        Object num1 = redisTemplate.opsForValue().get("num");
////        String num =(String) redisTemplate.opsForValue().get("num");
//        String num = redisTemplate.opsForValue().get("num");
//
//        //判断当前字符串
//        if (StringUtils.isEmpty(num)){
//            return;
//        }
//        //+1 操作
//        int numValue = Integer.parseInt(num);
//        redisTemplate.opsForValue().set("num" , String.valueOf(++numValue));
//    }

























