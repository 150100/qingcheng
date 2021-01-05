package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * ClassName:WxPayController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/6/23 22:28
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/wxpay")
public class WxPayController {

    @Reference
    private OrderService orderService;
    @Reference
    private WxPayService wxPayService;

    @GetMapping("/createNative")
    //点了“微信支付”后，从前端接收订单号，调用wxPayService返回给前端支付url去生成二维码
    public Map createNative(String orderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderService.findById(orderId);
        if (order != null) {
            if (username.equals(order.getUsername()) && "0".equals(order.getPayStatus()) && "0".equals(order.getOrderStatus())) {
                return wxPayService.createNative(orderId, order.getPayMoney(), "http://qingcheng.easy.echosite.cn/wxpay/notify.do");
            }else {
                return null;
            }
        }else{
            return null;
        }
    }

    @RequestMapping("/notify")
    //回调，接受微信支付平台返回的消息(xml)。最后内容解析与签名验证
    public void notifyLogic(HttpServletRequest request) {
        System.out.println("支付成功回调！");

        try {
            InputStream inputStream = request.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];//1024长度的byte数组
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) { //输入流读到数组中，不为空时
                outputStream.write(buffer, 0, len); //输出
            }
            outputStream.close();
            inputStream.close();
            String result = new String(outputStream.toByteArray(), "utf-8");//数组转化为字符串
            System.out.println(result);//字符串格式的xml
            wxPayService.notifyLogic(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
