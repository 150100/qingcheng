package com.qingcheng.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:LoginController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/4/12 11:43
 * @Author:jiaqi@163.com
 */
//获取登陆人的用户名
@RestController
@RequestMapping("/login")
public class LoginController {

    @GetMapping("/name")
    public Map showName() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Map map = new HashMap<>();
        map.put("name", name);
        return map;
    }
}
