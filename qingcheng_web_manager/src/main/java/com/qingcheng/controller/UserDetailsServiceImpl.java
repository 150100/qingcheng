package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.service.system.AdminService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName:UserDetailsServiceImpl
 * Package:com.itheima.demo
 * Description:
 *
 * @Date:2020/4/8 11:49
 * @Author:jiaqi@163.com
 */
//根据前端传过来的用户名，从数据库查到密文，和前端进行加密后的密文进行对比，一样即登陆成功。本实现类已在applicationContext_security.html配置过
public class UserDetailsServiceImpl implements UserDetailsService {

    @Reference
    private AdminService adminService;

    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        Map map = new HashMap<>();
        map.put("loginName", s);
        map.put("status", 1);
        List<Admin> list = adminService.findList(map);
        if (list.size() == 0) {
            return null;
        }

        //实际项目应该从数据库获取角色列表
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        //添加角色
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        //用户名，密码（数据库中的密文），角色
        return new User(s,list.get(0).getPassword(),grantedAuthorities);
    }
}
