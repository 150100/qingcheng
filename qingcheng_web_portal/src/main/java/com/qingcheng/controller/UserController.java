package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName:UserController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/5/23 9:58
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;

//    发送短信验证码
    @GetMapping("/sendSms")
    public Result sendSms(String phone) {
        userService.sendSms(phone);
        return new Result();
    }

//    用户注册
    @PostMapping("/save")
    public Result save(@RequestBody User user, String smsCode) {

        //密码加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newpassword = encoder.encode(user.getPassword());
        user.setPassword(newpassword);

        userService.add(user, smsCode);
        return new Result();
    }
}
