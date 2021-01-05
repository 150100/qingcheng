package com.qingcheng.controller;

import org.springframework.security.core.context.SecurityContext;
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
 * @Date:2020/5/30 10:36
 * @Author:jiaqi@163.com
 */

//获取用户名，写在公共模块，就不用每个service都写一遍了
@RestController
@RequestMapping("/login")
public class LoginController {

    @GetMapping("/username")
    public Map username() {
//        获取用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前用户名为：" + username);
        if ("anonymousUser".equals(username)) {  //如果当前未登录，用户名设置为空串，否则anonymousUser会显示在网页
            username = "";
        }
        Map map = new HashMap<>();
        map.put("username", username);
        return map;
    }
}
