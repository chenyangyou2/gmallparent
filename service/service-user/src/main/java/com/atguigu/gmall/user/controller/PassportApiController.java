package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    //  用户登录
    //  /api/user/passport/login
    //  UserInfo userInfo  springmvc 对象传值
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        //  调用服务层方法
        UserInfo info = userService.login(userInfo);
        //  判断
        if(info!=null){
            //  存在
            String token = UUID.randomUUID().toString();
            //  声明一个Map 集合
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("token",token);
            hashMap.put("nickName",info.getNickName());

            //  接下来将用户信息放入缓存！
            //  redis 数据类型，存储的内容
            //  key 如何起名：
            String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            //  ip地址.token 被盗用了，可能会存在安全隐患。
            //  存储一个ip地址进去。
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId",info.getId().toString());
            //  从缓存中获取userId 的时候，判断 存入的ip 与当前的ip 地址是否一致！如果一致，则返回userId ，不一致则说明非法登录.
            jsonObject.put("ip", IpUtil.getIpAddress(request));
            this.redisTemplate.opsForValue().set(loginKey,jsonObject.toJSONString(),RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            //  返回数据.
            return Result.ok(hashMap);
        }else {
            //  不存在
            return Result.fail().message("登录失败.");
        }
    }

    //  http://api.gmall.com/api/user/passport/logout
    @GetMapping("logout")
    public Result logout(HttpServletRequest request,@RequestHeader String token){
        //  删除cookie 中的数据 js 就自己操作了
        //  删除缓存数据
        //  第一种：
        //  String token = request.getHeader("token");
        //  第二种使用 注解
        String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
        this.redisTemplate.delete(loginKey);
        //  默认返回
        return Result.ok();
    }
}








//@RestController
//@RequestMapping("/api/user/passport")
//public class PassportApiController {
//
//    @Autowired
//    private UserService userService;
//    @Autowired
//    private RedisTemplate redisTemplate;
//    /**
//     * 登录
//     *  /api/user/passport/login
//      */
//    @PostMapping("login")
//    public Result login(UserInfo userInfo , HttpServletRequest request){
//        UserInfo info = userService.login(userInfo);
//        if (info != null){
//            // 存在
//            String token = UUID.randomUUID().toString();
//            HashMap<String, Object> hashMap = new HashMap<>();
//            hashMap.put("token",token);
//            hashMap.put("nickName" , info.getNickName());
//
//            // 将用户信息放入缓存
//            // redis 数据类型 储存的内容
//            String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
//            // ip 地址token 被盗用风险  储存一个Ip地址
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("userId" , info.getId().toString());
//            jsonObject.put("ip", IpUtil.getIpAddress(request));
//            redisTemplate.opsForValue().set(loginKey,info.getId().toString(),RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
//            return Result.ok(hashMap);
//        } else {
//            // 不存在
//            return Result.fail().message("登录失败");
//        }
//    }
//
//    /**
//     * 退出登录
//     * http://api.gmall.com/api/user/passport/logout
//      */
//    @GetMapping("logout")
//    public Result logout(HttpServletRequest request , @RequestHeader String token){
//        String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
//        redisTemplate.delete(loginKey);
//        return Result.ok();
//    }
//}
