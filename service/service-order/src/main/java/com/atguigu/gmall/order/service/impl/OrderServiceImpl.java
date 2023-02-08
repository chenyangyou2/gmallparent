package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderDetailVo;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.constant.MqConst;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.bouncycastle.asn1.dvcs.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper , OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl; // wareUrl = http://localhost:9001

    @Autowired
    private RabbitService rabbitService;

    @Override
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //  调用方法计算 总金额
        orderInfo.sumTotalAmount();
        // 赋值订单状态 订单默认未支付
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // out_trade_no 第三方交易编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(100);
        orderInfo.setOutTradeNo(outTradeNo);
        // 赋值订单主体
        orderInfo.setTradeBody("喝点84消毒液");
        // 赋值操作时间
        orderInfo.setOperateTime(new Date());
        // 设置一个过期时间 new Date（）+ 1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE , 1);
        orderInfo.setExpireTime(calendar.getTime());
        //设置订单的状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfoMapper.insert(orderInfo);

        //先保存 order_detail   在获取到 order_detail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                // 赋值订单Id
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
        //获取一个订单Id
        Long orderId = orderInfo.getId();

        // 保存订单，发送消息
        rabbitService.sendDelayMsg(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL , MqConst.ROUTING_ORDER_CANCEL , orderId , MqConst.DELAY_TIME);

        //返回订单Id
        return orderId;
    }























    @Override
    public String getTradeNo(String userId) {
        // 获取到流水号
        String tradeNo = UUID.randomUUID().toString();
        // 保存到缓存
        String tradeNoKey = "tradeNo:" + userId;
        // String 储存
        redisTemplate.opsForValue().set(tradeNoKey , tradeNo);
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        // 获取到流水号的key
        String tradeNoKey = "tradeNo:" + userId;
        // 获取缓存的流水号
        String tradeNoStr = (String) redisTemplate.opsForValue().get(tradeNoKey);

        return tradeNo.equals(tradeNoStr);
    }

    @Override
    public void delTradeNo(String userId) {
        // 获取到流水号的key
        String tradeNoKey = "tradeNo" + userId;
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }

    /**
     * 根据条件校验库存
     */
    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        //            http://localhost:9001/hasStock?skuId=10221&num=2
        //  wareUrl = http://localhost:9001
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //  0：无库存   1：有库存
        //  返回结果
        return "1".equals(result);
    }

    /**
     * 查看我的订单
     */
    @Override
    public IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId) {
        IPage<OrderInfo> infoIPage =  orderInfoMapper.selectOrderPage(orderInfoPage , userId);
        //  将订单状态赋值： orderStatus 赋值
        infoIPage.getRecords().forEach(orderInfo -> {
            //  UNPAID -- 未支付   PAID --- 已支付  --- orderStatusName
            //  orderInfo.setOrderStatus(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });
        return infoIPage;
    }

    /**
     * 监听取消订单
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        updateOrderStatus(orderId , ProcessStatus.CLOSED);
        //  发送消息，关闭交易记录.  发送消息的内容是
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setUpdateTime(new Date());
        orderInfoMapper.updateById(orderInfo);
    }

    /**
     * 根据订单Id 查询订单信息
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo != null){
            List<OrderDetail> orderDetails =
                    orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
            orderInfo.setOrderDetailList(orderDetails);
        }
        return orderInfo;
    }

    /**
     * 发送消息给库存系统，通知库存系统减库存
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        // 根据订单的进度状态 变为 已通知仓库
        updateOrderStatus(orderId , ProcessStatus.NOTIFIED_WARE);
        //根据订单Id 获取到orderInfo 数据
        //这个订单对象中，必须有订单明细集合数据
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //利用 orderInfo 中的部分数据组成json 字符串
        //将 orderInfo 中的部分字段 放入 map 集合中 然后将map 转化为json
        Map map = this.initWare(orderInfo);
        //发送Json 数据给库存
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_WARE_STOCK , MqConst.ROUTING_WARE_STOCK , JSON.toJSONString(map));
    }

    @Override
    public Map initWare(OrderInfo orderInfo) {
        //  声明一个map 集合
        HashMap<String, Object> hashMap = new HashMap<>();
        //  给map 集合赋值
        hashMap.put("orderId",orderInfo.getId());
        hashMap.put("consignee",orderInfo.getConsignee());
        hashMap.put("consigneeTel",orderInfo.getConsigneeTel());
        hashMap.put("orderComment",orderInfo.getOrderComment());
        hashMap.put("orderBody",orderInfo.getTradeBody());
        hashMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        hashMap.put("paymentWay","2");
        hashMap.put("wareId",orderInfo.getWareId()); // 拆单时，需要使用的字段。

        //  赋值订单明细
        //  先获取到订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //  推荐使用stream流
        List<HashMap<String, Object>> mapList = orderDetailList.stream().map(orderDetail -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("skuId", orderDetail.getSkuId());
            map.put("skuNum", orderDetail.getSkuNum());
            map.put("skuName", orderDetail.getSkuName());
            return map;
        }).collect(Collectors.toList());
        hashMap.put("details",mapList);
        //  返回数据
        return hashMap;
    }
    /**
     * 拆单接口
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
        1.  先知道谁要被拆   知道原始订单
        2.  wareSkuMap 这个参数变为可以使用的 java 对象  [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        3.  创建新的子订单并给子订单进行赋值
        4.  保存子订单
        5.  将子订单添加到集合中
        6.  修改原始订单状态
         */
        //  声明一个子订单集合对象
        ArrayList<OrderInfo> subOrderInfoList = new ArrayList<>();

        //  获取到原始订单: 包含订单主表信息 + 订单明细信息
        OrderInfo orderInfo = this.getOrderInfo(Long.parseLong(orderId));
        //  数据类型转换
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //  判断
        if (!CollectionUtils.isEmpty(mapList)){
            //  循环遍历
            for (Map map : mapList) {
                //  仓库Id
                String wareId = (String) map.get("wareId");
                List<String> skuIdList = (List<String>) map.get("skuIds");
                //  创建新的子订单并赋值.
                OrderInfo subOrderInfo = new OrderInfo();
                //  属性拷贝.
                BeanUtils.copyProperties(orderInfo,subOrderInfo);
                //  将id 设置为null,为了防止主键冲突
                subOrderInfo.setId(null);
                //  赋值仓库Id
                subOrderInfo.setWareId(wareId);
                //  赋值父订单Id
                subOrderInfo.setParentOrderId(Long.parseLong(orderId));

                ArrayList<OrderDetail> subOrderDetailList = new ArrayList<>();
                //  给子订单的订单明细集合赋值数据.
                List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    for (String skuId : skuIdList) {
                        //  判断
                        if (orderDetail.getSkuId().longValue() == Long.parseLong(skuId)){
                            //  记录当前子订单的明细
                            subOrderDetailList.add(orderDetail);
                        }
                    }
                }
                //  给子订单赋值
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                //  保存子订单数据
                this.saveOrderInfo(subOrderInfo);
                //  将子订单添加到集合中
                subOrderInfoList.add(subOrderInfo);
            }
        }

        //  修改原始订单状态
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        //  返回子订单集合数据
        return subOrderInfoList;
    }

    /**
     * 制定关闭订单数据
     */
    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //  更新订单状态.
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  判断是否需要关闭paymentInfo
        if ("2".equals(flag)){
            //  发送消息，关闭交易记录.  发送消息的内容是
            this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }

    }
}

















