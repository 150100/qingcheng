package com.qingcheng.consumer;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.http.MethodType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ClassName:SmsUtil
 * Package:com.qingcheng.consumer
 * Description:
 *
 * @Date:2020/5/25 11:52
 * @Author:jiaqi@163.com
 */

@Component
//发送短信
public class SmsUtil {

    @Value("${accessKeyId}")
    private String accessKeyId;
    @Value("${accessKeySecret}")
    private String accessKeySecret;

    public CommonResponse sendSms(String phone,String smsCode,String param) {
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", phone); //手机号
        request.putQueryParameter("SignName", "青橙商城"); //签名
        request.putQueryParameter("TemplateCode", smsCode); //模板代号
        request.putQueryParameter("TemplateParam", param); //模板参数，即具体内容 ${code}
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            return response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }
}
