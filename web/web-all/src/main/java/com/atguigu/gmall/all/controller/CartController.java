package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.PublicKey;

@Controller

public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 添加购物车
     * http://cart.gmall.com/addCart.html?skuId=26&skuNum=1&sourceType=query
     */
    @GetMapping("addCart.html")
    public String addToCart(HttpServletRequest request){
        // 获取skuId，获取skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        //获取到skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.parseLong(skuId));
        //保存数据
        request.setAttribute("skuInfo" , skuInfo);
        request.setAttribute("skuNum" , skuNum);

        // 返回购物车页面
        return "cart/addCart";
    }

    /**
     * cart.html
     */
    @GetMapping("cart.html")
    public String cartList(){
        // 返回购物车列表页面
        return "cart/index";
    }
}



























