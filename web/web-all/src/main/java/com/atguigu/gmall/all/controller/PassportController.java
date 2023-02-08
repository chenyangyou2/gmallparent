package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {
//    http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/

    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //获取从哪里跳转的url
        String originUrl = request.getParameter("originUrl");
        //保存
        request.setAttribute("originUrl" , originUrl);
        return "login";
    }

}
