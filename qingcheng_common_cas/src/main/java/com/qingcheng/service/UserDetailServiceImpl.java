package com.qingcheng.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName:UserDetailServiceImpl
 * Package:cn.itcast.demo.service
 * Description:
 *
 * @Date:2020/5/28 17:48
 * @Author:jiaqi@163.com
 */
public class UserDetailServiceImpl implements UserDetailsService {
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        List<GrantedAuthority> authorityList = new ArrayList<GrantedAuthority>();
        authorityList.add(new SimpleGrantedAuthority("ROLE_USER")); //添加角色

        return new User(username,"",authorityList); //不提供密码是因为用户名密码校验交给了cas，security只是赋予一下角色
    }
}
