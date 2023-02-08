package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import springfox.documentation.spring.web.json.Json;

import java.util.List;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/6/25 9:13
 * 描述：
 **/
@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Value("${authUrls.url}")
    private String authUrl; //  authUrl=trade.html,myOrder.html,list.html

    //  声明一个匹配格式对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 用户在访问业务的时候，先走过滤器
     * @param exchange  用户的web  请求与相应对象
     * @param chain 过滤器链
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //  先获取到用户发送的请求
        ServerHttpRequest request = exchange.getRequest();
        //  获取到当前的请求url ：/api/product/inner/getSkuInfo/24
        String path = request.getURI().getPath();
        //  判断是否属于内部数据接口
        if (antPathMatcher.match("/**/inner/**",path)){
            //  获取相应对象
            ServerHttpResponse response = exchange.getResponse();
            //  给信息提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  判断用户访问的请求中 是否带有/auth/ 如果带有，则必须要登录，没有登录的情况下访问，需要给提示信息.
        //  在缓存中获取用户Id
        String userId = this.getUserId(request);
        //  获取临时用户Id
        String userTempId = this.getUserTempId(request);

        //  判断  判断ip 地址是否相同，如果不相同则返回-1
        if ("-1".equals(userId)){
            //  获取相应对象
            ServerHttpResponse response = exchange.getResponse();
            //  给信息提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  判断 在订单的时候，才会编写这样的请求地址。
        if (antPathMatcher.match("/api/**/auth/**",path)){
            //  判断当前是否登录
            if (StringUtils.isEmpty(userId)){
                //  获取相应对象
                ServerHttpResponse response = exchange.getResponse();
                //  给信息提示
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //  authUrl=trade.html,myOrder.html,list.html
        String[] split = authUrl.split(",");
        if (split!=null && split.length>0){
            for (String url : split) {
                //  判断path 中是否包含url
                //  http://list.gmall.com/list.html?category3Id=61 但是用户Id 为空 说明没有登录.  跳转到登录页面。
                if (path.indexOf(url)!=-1 && StringUtils.isEmpty(userId)){
                    //  获取到相应对象
                    ServerHttpResponse response = exchange.getResponse();
                    //  重定向到哪里？
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //  request.getURI() = http://list.gmall.com/list.html?category3Id=61
                    //  request.getURI().getPath() = list.html?category3Id=61
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl="+request.getURI());
                    //  重定向到登录页面
                    return response.setComplete();
                }
            }
        }
        //  将用户Id 放入请求头，目的是让后台的微服务模块，直接通过请求头方式获取即可！
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)){
            //  特殊的存储方式
            if (!StringUtils.isEmpty(userId)){
                //  request.getHeaders().set("userId",userId); 设置失败;
                //  设置完成之后，返回 Mono<Void> 对象
                //  返回  ServerHttpRequest   当前 request 的类型 ServerHttpRequest
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)){
                //  返回  ServerHttpRequest  userId1  当前 request 的类型 ServerHttpRequest
                request.mutate().header("userTempId", userTempId).build();
            }
            //  将 request --> exchange
            return chain.filter(exchange.mutate().request(request).build());
        }
        //  默认返回，但是这个exchange 中没有添加任何的请求头数据.
        return chain.filter(exchange);
    }

    /**
     * 获取临时用户Id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        //  定义一个临时用户Id
        String userTempId = "";
        //  两处 cookie  或  header 中！
        List<String> stringList = request.getHeaders().get("userTempId");
        if (!CollectionUtils.isEmpty(stringList)){
            userTempId = stringList.get(0);
        } else {
            //    从cookie 中获取数据
            HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
            if (httpCookie!=null){
                userTempId = httpCookie.getValue();
            }
        }
        //  返回临时用户Id
        return userTempId;
    }

    /**
     * 用户输出提示方法
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //  输出的内容都在 resultCodeEnum 这个对象中
        Result<Object> result = Result.build(null, resultCodeEnum);
        //  现在要输出的内容都在result 对象中
        String printStr = JSON.toJSONString(result);
        //  打印相当于用数据流写出去.
        DataBuffer wrap = response.bufferFactory().wrap(printStr.getBytes());
        //  设置请求头格式 java-web
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //  返回数据。
        return response.writeWith(Mono.just(wrap));
    }

    /**
     * 获取用户Id
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        //  用户Id 存储在缓存中
        String token = "";
        //  token 存在cookie 或 请求头
        List<String> stringList = request.getHeaders().get("token");
        //  判断集合不为空
        if (!CollectionUtils.isEmpty(stringList)){
            //  存储数据的时候只放入了一个值，因此获取数据的时候下标是 0
            token = stringList.get(0);
        }else {
            //  从cookie 中获取 token --> 对应的存储数据只有一条
            HttpCookie token2 = request.getCookies().getFirst("token");
            if (token2!=null){
                token = token2.getValue();
            }
        }

        //  从缓存中获取数据
        if (!StringUtils.isEmpty(token)){
            //  组成缓存的key
            String loginKey = "user:login:"+token;
            //  获取数据
            String strJson = (String) this.redisTemplate.opsForValue().get(loginKey);
            //  存储的数据类型进行转换
            JSONObject jsonObject = JSONObject.parseObject(strJson);
            if (jsonObject!=null){
                //  登录时从缓存中获取的ip 地址。
                String ip = (String) jsonObject.get("ip");
                //  ip 地址相等则返回用户Id
                if (ip.equals(IpUtil.getGatwayIpAddress(request))){
                    //  获取userId
                    String userId = (String) jsonObject.get("userId");
                    return userId;
                }else {
                    //  返回-1 ：表示非法登录
                    return "-1";
                }
            }
        }
        return "";
    }
}



























































//package com.atguigu.gmall.gateway.filter;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.atguigu.gmall.common.result.Result;
//import com.atguigu.gmall.common.result.ResultCodeEnum;
//import com.atguigu.gmall.common.util.IpUtil;
//import org.apache.http.HttpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.HttpCookie;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Component
//public class AuthGlobalFilter implements GlobalFilter {
//
//    @Value("${authUrls.url}")
//    private String authUrl; //url: trade.html,myOrder.html,list.html
//
//    @Autowired
//    private AntPathMatcher antPathMatcher;
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    /**
//     * 用户访问业务的时候，先走过滤器
//     */
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 获取用户发送的请求
//        ServerHttpRequest request = exchange.getRequest();
//        // 获取当前的请去url
//        String path = request.getURI().getPath();
//        // 判断是否属于内部数据接口
//        if (antPathMatcher.match("/**/inner/**" , path)){
//            // 获取响应对象
//            ServerHttpResponse response = exchange.getResponse();
//            // 给信息提示
//            return out(response , ResultCodeEnum.PERMISSION);
//        }
//
//        // 判断用户访问请求中是否带有/auth/ 如果有需要登录，没有登录的情况下访问，需要给提示信息
//        // 在缓存中获取用户Id
//        String userId = getUserId(request);
//        // 判断 ip 地址是否相同，如果不同返回-1
//        if ("-1".equals(userId)){
//            // 获取相应对象
//            ServerHttpResponse response = exchange.getResponse();
//            // 给提示信息
//            return out(response,ResultCodeEnum.PERMISSION);
//        }
//        // 判断
//        if (antPathMatcher.match("/api/**/auth/**",path)){
//            // 判断是否登录
//            if (StringUtils.isEmpty(userId)){
//                // 获取响应对象
//                ServerHttpResponse response = exchange.getResponse();
//                // 给信息提示
//                return out(response , ResultCodeEnum.LOGIN_AUTH);
//            }
//        }
//
//        //url: trade.html,myOrder.html,list.html
//        String[] split = authUrl.split(",");
//        if (split != null && split.length > 0){
//            for (String url : split) {
//                // 判断path 中是否包含url
//                if (path.indexOf(url) != -1 && StringUtils.isEmpty(userId)){
//                    // 获取到相应的对象
//                    ServerHttpResponse response = exchange.getResponse();
//                    // 重定向到哪里
//                    response.setStatusCode(HttpStatus.SEE_OTHER);
//                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl=" + request.getURI());
//                    return response.setComplete();
//                }
//            }
//        }
//        //
//        if (!StringUtils.isEmpty(userId)){
//            request.mutate().header("userId" , userId).build();
//
//            return chain.filter(exchange.mutate().request(request).build());
//        }
//        return chain.filter(exchange);
//    }
//
//    /**
//     * 用户输出提示
//     * @param response
//     * @param resultCodeEnum
//     * @return
//     */
//    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
//        //
//        Result<Object> result = Result.build(null , resultCodeEnum);
//        //
//        String printStr = JSON.toJSONString(result);
//        //
//        DataBuffer wrap = response.bufferFactory().wrap(printStr.getBytes());
//        //
//        response.getHeaders().add("Content-Type" , "application/json;charset=UTF8");
//
//        return response.writeWith(Mono.just(wrap));
//    }
//
//    /**
//     * 获取用户Id
//     * @param request
//     * @return
//     */
//    private String getUserId(ServerHttpRequest request) {
//        // 用户Id
//        String token = "";
//        // token 存在cookie 或 请求头中
//        List<String> stringList = request.getHeaders().get("token");
//        // 判断集合不为空
//        if (!CollectionUtils.isEmpty(stringList)){
//            // 储存数据的时候只放一个值， 一次获取数据下标是0
//            token = stringList.get(0);
//        }else {
//            //
//            HttpCookie token2 = request.getCookies().getFirst("token");
//            if (token2 != null){
//                token = token2.getValue();
//            }
//        }
//        // 从缓存中获取数据
//        if (!StringUtils.isEmpty(token)){
//            // 组成缓存的key
//            String loginKey = "user:login:" + token;
//            // 获取数据
//            String strJson = (String) redisTemplate.opsForValue().get(loginKey);
//            // 储存数据类型进行 转换
//            JSONObject jsonObject = JSONObject.parseObject(strJson);
//            if (jsonObject != null){
//                //  登陆时从缓存中获取ip地址
//                String ip = (String) jsonObject.get("ip");
//                // ip 地址相同则返回ip 地址
//                if (ip.equals(IpUtil.getGatwayIpAddress(request))){
//                    String userId = (String) jsonObject.get("userId");
//                    return userId;
//                }else {
//                    // 返回 -1 ：表示非法登录
//                    return "-1";
//                }
//            }
//        }
//        return "";
//    }
//}
