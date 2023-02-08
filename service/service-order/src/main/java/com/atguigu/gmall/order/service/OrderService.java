package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {
    /**
     * 提交订单
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 获取到流水号
     */
    String getTradeNo(String userId);

    /**
     * 流水号比较
     */
    Boolean checkTradeNo(String tradeNo , String userId);

    /**
     * 删除缓存流水号
     */
    void delTradeNo(String userId);

    /**
     * 根据条件校验库存
     */
    Boolean checkStock(Long skuId , Integer skuNum);

    /**
     * 查看我的订单
     */
    IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId);

    /**
     * 监听取消订单
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单Id 修改订单 进度状态
     */
    void updateOrderStatus(Long orderId, ProcessStatus closed);

    /**
     * 根据订单Id 查询订单信息
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 发送消息给库存系统，通知库存系统减库存
     */
    void sendOrderStatus(Long orderId);

    /**
     * 订单对象变Map集合
     */
    Map initWare(OrderInfo orderInfo);

    /**
     * 拆单接口
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 制定关闭订单数据
     */
    void execExpiredOrder(Long orderId, String flag);
}
