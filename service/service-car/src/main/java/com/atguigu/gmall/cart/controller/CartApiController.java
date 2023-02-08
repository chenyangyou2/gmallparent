package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     * /api/cart/addToCart/{skuId}/{skuNum}
     */
    @GetMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){
        // 获取请求头userId
        String userId = AuthContextHolder.getUserId(request);
        // 如果用户未登录 获取去临时用户id
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId , userId , skuNum);
        return Result.ok();
    }

    /**
     * 查询购物车列表
     * /api/cart/cartList
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
         List<CartInfo> cartInfoList = cartService.getCartList(userId , userTempId);
         return Result.ok(cartInfoList);
    }

    /**
     * 删除购物车
     * /api/cart/deleteCart/{skuId}
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId , HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId , userId);
        return Result.ok();
    }

    /**
     * 跟新选中状态
     * /api/cart/checkCart/{skuId}/{isChecked}
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        // 获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(skuId , userId , isChecked);
        return Result.ok();
    }

    /**
     * 获取选中状态为1 的购物车商品集合(发布数据接口)
     * /api/cart/getCartCheckedList/{userId}
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
}
















