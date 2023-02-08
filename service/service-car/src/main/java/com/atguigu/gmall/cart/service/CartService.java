package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车
     */
    void addToCart(Long skuId , String userId , Integer skuNum);

    /**
     * 查询购物车列表
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 删除购物车
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 跟新选中状态
     */
    void checkCart(Long skuId, String userId, Integer isChecked);

    /**
     * 获取选中状态为1 的购物车商品集合
     */
    List<CartInfo> getCartCheckedList(String userId);
}
