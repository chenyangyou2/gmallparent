package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {

        //定义购物车的key=user:userId:cart
        String cartKey = getCartKey(userId);
        //获取购物车中是否有该商品
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfoExist != null){
            // 购物车中有这个商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 默认状态 0 表示没有选中，1 表示选中
            if (cartInfoExist.getIsChecked().intValue() == 0){
                cartInfoExist.setIsChecked(1);
            }
            //赋值商品最新价格  sku_info.price
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //覆盖时间
            cartInfoExist.setUpdateTime(new Date());

            //  将最新的数据写回缓存
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        } else {
            // 当购物车不存在这个商品时
            // 远程调用
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            // 创建对象
            cartInfoExist = new CartInfo();
            cartInfoExist.setUserId(userId);
            cartInfoExist.setSkuId(skuId);
            cartInfoExist.setCartPrice(this.productFeignClient.getSkuPrice(skuId));
            cartInfoExist.setSkuNum(skuNum);
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            cartInfoExist.setSkuPrice(this.productFeignClient.getSkuPrice(skuId));
            cartInfoExist.setCreateTime(new Date());
            cartInfoExist.setUpdateTime(new Date());

            //   cartInfoExist = cartInfo;
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo);
        }
        // 将数据储存到缓存中
        redisTemplate.opsForHash().put(cartKey , skuId.toString() , cartInfoExist);
    }

    /**
     * 定义购物车的key
     */
    private String getCartKey(String userId) {
        String cartKey = RedisConst.USER_KEY_PREFIX + userId +RedisConst.USER_CART_KEY_SUFFIX;
        return cartKey;
    }

    @Override
    /**
     * 查询购物车列表
     */
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 先声明一个集合来储存购物车数据
        List<CartInfo> noLoginCartInfoList = new ArrayList<>();

        // 临时用户Id 不为空
        if (!StringUtils.isEmpty(userTempId)){
            // 先获取购物车的key
            String cartKey = getCartKey(userTempId);
            // 获取临时的购物集合数据
            noLoginCartInfoList = redisTemplate.opsForHash().values(cartKey);
        }
        // 判断userId是空，同时未登录集合数据不为空
        if (StringUtils.isEmpty(userId) && !CollectionUtils.isEmpty(noLoginCartInfoList)){
            // 查看购物车列表的时候，因该按照购物车的更新时间进行排序
            // comparator 之定义比较器
            noLoginCartInfoList.sort((c1 , c2) -> {
                return DateUtil.truncatedCompareTo(c2.getUpdateTime() , c1.getUpdateTime() , Calendar.SECOND);
            });
            return noLoginCartInfoList;
        }

        // 声明一个登录购物车集合
        List<CartInfo> loginCartInfoList = new ArrayList<>();
        // 判断
        if (!StringUtils.isEmpty(userId)){
            // 说明用户已登录   先获取购物车的key
            String cartKey = getCartKey(userId);
            // 获取登陆时购物车的集合 hvals key
            // 1.合并的时候，需要根据skuId 是否相等作为合并条件
            // 2.登录的购物车集合中，是否包含未登录的skuId
            BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
            if (!CollectionUtils.isEmpty(noLoginCartInfoList)){
                // 未登录购物车进行遍历
                noLoginCartInfoList.stream().forEach(noLoginCartInfo -> {
                    if (boundHashOperations.hasKey(noLoginCartInfo.getSkuId().toString())){
                        //  商品的数量相加
                        //  先获取到登录的数据
                        CartInfo loginCartInfo = boundHashOperations.get(noLoginCartInfo.getSkuId().toString());
                        loginCartInfo.setSkuNum(loginCartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
                        //修改更新时间
                        loginCartInfo.setUpdateTime(new Date());
                        //为了保证价格的实时性
                        loginCartInfo.setSkuPrice(productFeignClient.getSkuPrice(loginCartInfo.getSkuId()));

                        // 有选中状态的问题
                        if (noLoginCartInfo.getIsChecked().intValue() == 1){
                            // 设置选中状态
                            if (loginCartInfo.getIsChecked().intValue() == 0){
                                loginCartInfo.setIsChecked(1);
                            }
                        }
                        // 保存数据到缓存：
                        boundHashOperations.put(noLoginCartInfo.getSkuId().toString() , loginCartInfo);
                    } else {
                        if (noLoginCartInfo.getIsChecked().intValue() == 1){
                            // 直接写入到登录的购物车中 25
                            //  将原来的未登录userId 设置为登录的userId 111 ---> 1
                            noLoginCartInfo.setUserId(userId);
                            noLoginCartInfo.setCreateTime(new Date());
                            noLoginCartInfo.setUpdateTime(new Date());
                            //  看需求
                            //  noLoginCartInfo.setSkuPrice(this.productFeignClient.getSkuPrice(loginCartInfo.getSkuId()));
                            //  this.redisTemplate.opsForHash().put(cartKey,noLoginCartInfo.getSkuId().toString(),noLoginCartInfo);
                            redisTemplate.boundHashOps(cartKey).put(noLoginCartInfo.getSkuId().toString() , noLoginCartInfo);
                        }
                    }
                });
                // 删除未登录购物车数据
                redisTemplate.delete(getCartKey(userTempId));
            }
            // 登录购物车中有数据了，要查询到最新的合并结果
            loginCartInfoList = boundHashOperations.values();
            // 判断当前集合为空
            if (CollectionUtils.isEmpty(loginCartInfoList)){
                return new ArrayList<>();
            }
            // 按照更新时间排序
            loginCartInfoList.sort((c1 , c2) -> {
                return DateUtil.truncatedCompareTo(c2.getUpdateTime() , c1.getUpdateTime() , Calendar.SECOND);
            });
        }
        return loginCartInfoList;
    }

    @Override
    /**
     * 删除购物车
     */
    public void deleteCart(Long skuId, String userId) {
        // 获取购物车的key
        String cartKey = getCartKey(userId);
        // 删除购物车
        redisTemplate.opsForHash().delete(cartKey,skuId.toString());

    }

    @Override
    /**
     * 跟新选中状态
     */
    public void checkCart(Long skuId, String userId, Integer isChecked) {
        // 获取购物车的key
        String cartKey = getCartKey(userId);
        // 获取到修改的商品
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey , skuId.toString());
        if (cartInfo != null){
            // 赋值修改状态
            cartInfo.setIsChecked(isChecked);
            // 将修改之后的数据在保存到缓存
            redisTemplate.opsForHash().put(cartKey , skuId.toString() , cartInfo);
        }
    }

    @Override
    /**
     * 获取选中状态为1 的购物车商品集合
     */
    public List<CartInfo> getCartCheckedList(String userId) {
        // 先获取购物车列表
        // 先获取购物车列的key
        String cartKey = getCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);

        List<CartInfo> cartInfoCheckList = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        return cartInfoCheckList;
    }
}






















































