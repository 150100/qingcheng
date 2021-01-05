package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.Config;
import com.github.wxpay.sdk.WXPayRequest;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:WxPayServiceImpl
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/6/23 21:49
 * @Author:jiaqi@163.com
 */
@Service
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private Config config;
    @Autowired
    private OrderService orderService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    //从前端接收订单号，实付金额，回调地址。返回给前端支付url去生成二维码
    public Map createNative(String orderId, Integer money, String notifyUrl) {

        try {
            //1.封装请求参数
            Map<String, String> map = new HashMap();
            map.put("appid", config.getAppID());//公众号id
            map.put("mch_id", config.getMchID());//商户号
            map.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
            map.put("body","青橙");//商品描述
            map.put("out_trade_no", orderId);//订单号
            map.put("total_fee", money + "");//金额（分）
            map.put("spbill_create_ip","127.0.0.1");//终端IP
            map.put("notify_url", notifyUrl);//回调地址
            map.put("trade_type","NATIVE");//交易类型

            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());//将map转换成xml格式的参数
            System.out.println("参数：" + xmlParam);

            //2.发送请求
            WXPayRequest wxPayRequest = new WXPayRequest(config);
            String xmlRequest = wxPayRequest.requestWithCert("/pay/unifiedorder", null, xmlParam, false);//url后缀、无用、xml格式的参数、自动报告
            System.out.println("结果：" + xmlRequest);

            //3.解析返回结果
            Map<String,String> mapResult = WXPayUtil.xmlToMap(xmlRequest);
            Map m = new HashMap();
            m.put("code_url", mapResult.get("code_url")); //支付url，用于生成二维码
            m.put("total_fee", money + "");
            m.put("out_trade_no", orderId);
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap();
        }
    }

    @Override
    //内容解析与签名验证
    public void notifyLogic(String xml) {

        try {
            //1.对xml进行解析
            Map<String, String> map = WXPayUtil.xmlToMap(xml);
            //2.验证签名
            boolean signatureValid = WXPayUtil.isSignatureValid(map, config.getKey());

            System.out.println("验证签名是否正确：" + signatureValid);
            System.out.println(map.get("out_trade_no"));
            System.out.println(map.get("result_code"));//是否执行成功
            //3.修改订单状态
            if (signatureValid) {
                if ("SUCCESS".equals(map.get("result_code"))) {
                    orderService.updatePayStatus(map.get("out_trade_no"), map.get("transaction_id"));//订单号、流水号
                    //给rabbitmq发送订单号
                    rabbitTemplate.convertAndSend("paynotify","",map.get("out_trade_no"));
                } else {
                    //记录日志
                }
            } else {
                //记录日志
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
