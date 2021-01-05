package com.qingcheng.service.order;

import java.util.Map;

/**
 * ClassName:WxPayService
 * Package:com.qingcheng.service.order
 * Description:
 *
 * @Date:2020/6/23 18:28
 * @Author:jiaqi@163.com
 */

public interface WxPayService {
    //从前端接收订单号，实付金额，回调地址。返回给前端支付url去生成二维码
    public Map createNative(String orderId, Integer money, String notifyUrl);

    //内容解析与签名验证
    public void notifyLogic(String xml);
}
