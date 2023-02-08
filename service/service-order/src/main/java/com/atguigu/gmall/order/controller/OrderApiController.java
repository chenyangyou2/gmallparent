package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 订单结算页
     * /api/order/auth/trade
     */
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);

        HashMap<String, Object> map = new HashMap<>();
        // 获取用户收货地址类别
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        // 获取送货清单数据
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 什么一个变量来存商品的总件数
        AtomicInteger totalNum = new AtomicInteger();
        // 需要将数据转换为OrderDetail 集合
        List<OrderDetail> orderDetailArrayList = cartCheckedList.stream().map(cartInfo -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());

            // 计算总件数
            totalNum.addAndGet(orderDetail.getSkuNum());
            return orderDetail;
        }).collect(Collectors.toList());

        OrderInfo orderInfo = new OrderInfo();
        // 一定要赋值订单明细集合
        orderInfo.setOrderDetailList(orderDetailArrayList);
        // 调用计算总金额方法
        orderInfo.sumTotalAmount();

        // 存储数据
        map.put("userAddressList" , userAddressList);
        // 不直接存储 cartCheckedList集合 因为页面渲染的 orderPrice 字段，在cartInfo 中不存在
        map.put("detailArrayList" , orderDetailArrayList);
        // 保存总件数
        map.put("totalNum" , totalNum);
        // 计算总金额
        map.put("totalAmount" , orderInfo.getTotalAmount());

        // 存一个流水号 ${tradeNo}
        // 调用一个方法获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo" , tradeNo);

        return Result.ok(map);
    }
    /**
     * 提交订单
     * /api/order/auth/submitOrder
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 赋值用户Id
        orderInfo.setUserId(Long.parseLong(userId));

        //获取页面传递过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法
        Boolean result = orderService.checkTradeNo(tradeNo, userId);
        if (!result){
            // 不能提交订单，并提示
            return Result.fail().message("不能无刷新重复提交订单");
        }


        //创建一个集合对象
        ArrayList<CompletableFuture> futureList = new ArrayList<>();
        //创建一个信息提示
        ArrayList<String> errorList = new ArrayList<>();



        // 调用校验库存
        for (OrderDetail orderDetail : orderInfo.getOrderDetailList()){

            // 创建一个CompletableFuture
            CompletableFuture<Void> checkCompletableFuture = CompletableFuture.runAsync(() -> {
                // 校验库存方法
                Boolean exist = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!exist) {
                    // 说明么有库存了
                    errorList.add(orderDetail.getSkuName() + "没有足够库存");
                }
            },threadPoolExecutor);
            futureList.add(checkCompletableFuture);


            CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                //每个商品都要验证
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                //商品最新价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                // 比较价格是否一致
                String msg = orderPrice.compareTo(skuPrice) == 1 ? "降价啦！！" : "涨价啦！！！";
                if (orderPrice.compareTo(skuPrice) != 0) {
                    //有变动  变动金格
                    BigDecimal abs = orderPrice.subtract(skuPrice).abs();

                    //  如果价格有变动，那么可以将最新价格同步到购物车.
                    String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                    //  hget key field;
                    CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, orderDetail.getSkuId().toString());
                    cartInfo.setSkuPrice(skuPrice);
                    //  hset key field value
                    this.redisTemplate.opsForHash().put(cartKey, orderDetail.getSkuId().toString(), cartInfo);

                    errorList.add(orderDetail.getSkuName() + msg + abs);
                }
            },threadPoolExecutor);
            futureList.add(priceCompletableFuture);
        }


        // 将多线程执行的结果经行任务组合
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        // 判断当前是否需要显示提示信息
        if (errorList.size() > 0){
            //需要
            return Result.fail().message(StringUtils.join(errorList , ","));
        }

        Long orderId = orderService.saveOrderInfo(orderInfo);

        // 删除缓存的流水
        orderService.delTradeNo(userId);

        return Result.ok(orderId);
    }

    /**
     * 查看我的订单
     * /api/order/auth/{page}/{limit}
     */
    @GetMapping("auth/{page}/{limit}")
    public Result getOrderPage(@PathVariable Long page,
                               @PathVariable Long limit,
                               HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 分装page 对象
        Page<OrderInfo> orderInfoPage = new Page<>();
        // 调用服务层
        IPage<OrderInfo> iPage = orderService.getOrderPage(orderInfoPage , userId);

        return Result.ok(iPage);
    }

    /**
     * 根据订单Id 查询订单信息{有orderInfo  +  orderDetail}
     * /auth/trade/inner/getOrderInfo/{orderId}
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){

        return orderService.getOrderInfo(orderId);
    }

    //  http://localhost:8204/api/order/orderSplit
    @PostMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        //  获取参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //  调用服务层方法
        List<OrderInfo> subOrderInfoList = this.orderService.orderSplit(orderId,wareSkuMap);
        List<Map> list = new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            //  将orderInfo 变为map
            Map map = this.orderService.initWare(orderInfo);
            list.add(map);
        }
        //  返回数据
        return JSON.toJSONString(list);
    }

}





















