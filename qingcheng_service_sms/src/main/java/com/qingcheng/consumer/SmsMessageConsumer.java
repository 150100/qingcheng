package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * ClassName:SmsMessageConsumer
 * Package:com.qingcheng.consumer
 * Description:
 *
 * @Date:2020/5/23 10:38
 * @Author:jiaqi@163.com
 */
//消息监听器,获取队列中的消息。单独启动qingcheng_service_sms的tomcat就可以，本模块与本项目是解耦的，不需要依赖web模块的tomcat
public class SmsMessageConsumer implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;

    @Value("${smsCode}")
    private String smsCode;
    @Value("${param}")
    private String param;

    @Override
    public void onMessage(Message message) {
        String jsonString = new String(message.getBody()); //把数组转化成字符串
        Map<String,String> map = JSON.parseObject(jsonString, Map.class); //再把字符串转化为Map
        String code = map.get("code");
        String phone = map.get("phone");
        System.out.println("手机号：" + phone + " 验证码：" + code);

        //阿里云通信
        smsUtil.sendSms(phone, smsCode, param.replace("[value]",code));
    }
}
