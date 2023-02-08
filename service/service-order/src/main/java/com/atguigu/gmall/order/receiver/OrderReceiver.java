package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.mq.constant.MqConst;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    /**
     * 监听取消订单
     */
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId , Message message , Channel channel) throws IOException {

        try {
            if (orderId != null){
                // 执行取消订单方法 ， 订单状态必须未支付！
                OrderInfo orderInfo = orderService.getById(orderId);
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())) {

                    //  现在要判断一下 paymentInfo 是否有数据！在此需要远程调用查询paymentInfo 记录
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    if (paymentInfo!=null){
                        //  是否需要关闭支付宝的内部交易记录? 先调用查询交易记录方法.
                        Boolean exist = this.paymentFeignClient.checkPayment(orderId);
                        //  有交易记录，才有可能关闭支付宝的记录.如果都没有记录产生，则都不需要调用 关闭支付宝交易记录.
                        if (exist){
                            //  调用关闭支付宝交易记录
                            Boolean result = paymentFeignClient.closePay(orderId);
                            //  判断
                            if (result){
                                //  result = true, 说明关闭成功了. 还需要把 orderInfo ,paymentInfo 关闭！
                                orderService.execExpiredOrder(orderId,"2");
                            } else {
                                //  result = false, 说明关闭失败！ 说明支付成功。
                            }
                        }else {
                            //  直接关闭 orderInfo and paymentInfo
                            //  orderInfo 不为空， paymentInfo也不为空 ，这两个都需要关闭!
                            orderService.execExpiredOrder(orderId,"2");
                        }
                    } else {
                        //  orderInfo 不为空， paymentInfo 空 ，在此只需要关闭 orderInfo
                        //  直接将订单状态改为关闭！
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag() , false);
    }

    /**
     * 监听支付成功发送过来的消息
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderStatus(Long orderId,Message message,Channel channel){
        try {
            //  判断
            if (orderId!=null){
                // 更新订单状态PAID
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                // 发送消息给库存系统，通知库存系统减库存
                orderService.sendOrderStatus(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 设置监听减库存消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER , durable = "true" , autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void upOrderStatus(String json , Message message , Channel channel) throws IOException {
        try {
            // 判断
            if (!StringUtils.isEmpty(json)){
                Map map = JSON.parseObject(json , Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");

                if ("DEDUCTED".equals(status)){
                    // 说明减库存成功，更改订单状态
                    orderService.updateOrderStatus(Long.parseLong(orderId) , ProcessStatus.WAITING_DELEVER);
                }else {
                    // 减库存异常，更改订单状态
                    orderService.updateOrderStatus(Long.parseLong(orderId) , ProcessStatus.STOCK_EXCEPTION);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag() , false);
    }
}


















