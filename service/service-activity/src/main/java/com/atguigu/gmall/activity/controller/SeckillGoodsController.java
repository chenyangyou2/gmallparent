package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.constant.MqConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 获取全部数据
     * /api/activity/seckill/findAll
     */
    @GetMapping("findAll")
    public Result findAll(){
        List<SeckillGoods> seckillGoodsList = seckillGoodsService.findAll();
        return Result.ok(seckillGoodsList);
    }

    /**
     * 点击立即抢购获取到秒杀详情
     * /api/activity/seckill/getSeckillGoods/{skuId}
     */
    @GetMapping("getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        return Result.ok(seckillGoods);
    }

    /**
     * 获取下单码
     * /api/activity/seckill/auth/getSeckillSkuIdStr/{skuId}
     */

    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId , HttpServletRequest request){
        //获取到用户id 为什么用userId 不用skuId
        String userId = AuthContextHolder.getUserId(request);
        //生成的时机是 秒杀开始之后，结束之前
        SeckillGoods seckillGoods = this.seckillGoodsService.getSeckillGoods(skuId);
        if (seckillGoods!=null){
            Date currentTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(),currentTime) &&
                    DateUtil.dateCompare(currentTime,seckillGoods.getEndTime())){
                //  对 userId 进行 MD5 加密.
                String skuIdStr = MD5.encrypt(userId);
                //  返回下单码.
                return Result.ok(skuIdStr);
            }
        }
        // 生成下单码失败
        return Result.fail().message("生成下单码失败");
    }
    //  /api/activity/seckill/auth/seckillOrder/{skuId}
    //  this.api_name + '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr,
    //  表示秒杀前的业务判断。
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        /*
        1.  校验下单码
        2.  校验状态位
        3.  如果上述验证通过，将数据发送到 mq 中!
         */
        String skuIdStr = request.getParameter("skuIdStr");
        //  获取到userId
        String userId = AuthContextHolder.getUserId(request);
        //  校验
        if (!skuIdStr.equals(MD5.encrypt(userId))){
            //  返回信息提示
            return Result.fail().message("下单码校验失败，非法秒杀");
        }

        //  校验状态位 利用工具类获取状态位
        //  key = skuId
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            //  返回信息提示
            return Result.fail().message("表示当前商品已售罄");

        }else if (StringUtils.isEmpty(status)){
            //  如果是空，则说明非法请求.
            return Result.fail().message("状态位为null,非法请求");
        } else {
            //  说明当前商品可以秒杀。 需要记录当前哪个用户，要购买哪个商品
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);

            //  将这个对象放入队列中.
            rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
            //  返回
            return Result.ok();
        }
    }
}
