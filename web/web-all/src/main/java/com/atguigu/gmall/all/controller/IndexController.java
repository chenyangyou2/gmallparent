package com.atguigu.gmall.all.controller;

import com.alibaba.nacos.common.model.core.IResultCode;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping({"/","index.html"})
    public String index(Model model){
        Result result = productFeignClient.getBaseCategoryList();
        // ${list}
        model.addAttribute("list" , result.getData());
        //返回首页模板
        return "index/index";
    }

    // 制作一个静态化：
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        // 远程调用一个productFeignClient 获取到 数据
        Result result = productFeignClient.getBaseCategoryList();
        // 创建IContext 对象
        Context context = new Context();
        // 相当于储存key 对应的数据
        context.setVariable("list" , result.getData());
        // 创建 Writer 对象
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\zzzz\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 注入一个引擎模板
        templateEngine.process("index/index.html" , context ,fileWriter );
        // 默认返回
        return Result.ok();
    }











}
