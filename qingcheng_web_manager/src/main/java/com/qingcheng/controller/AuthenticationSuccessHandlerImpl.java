package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.util.WebUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.persistence.Id;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * ClassName:AuthenticationSuccessHandlerImpl
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/4/12 17:39
 * @Author:jiaqi@163.com
 */
//实现springsecurity提供登录成功处理器，可以实现在登录后进行的后续处理逻辑，还需要applicationContext_security.html对本实现类的相关配置
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
//        登陆后会调用
        System.out.println("登陆成功处理器执行啦！");

        String loginName = authentication.getName();
        String ip = httpServletRequest.getRemoteAddr();

        LoginLog loginLog = new LoginLog();
        loginLog.setLoginName(loginName); //当前登陆管理员
        loginLog.setLoginTime(new Date()); //当前登陆时间
        loginLog.setIp(ip); //远程客户端ip
        loginLog.setLocation(WebUtil.getCityByIP(ip)); //地区
        String agent = httpServletRequest.getHeader("user-agent"); //获取头部信息，这个字符串固定的
        System.out.println("agent:" + agent);
        loginLog.setBrowserName(WebUtil.getBrowserName(agent)); //浏览器名称

        loginLogService.add(loginLog);

        httpServletRequest.getRequestDispatcher("main.html").forward(httpServletRequest,httpServletResponse);
    }
}
