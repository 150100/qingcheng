package com.qingcheng.service.goods;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

/**
 * ClassName:StockBackService
 * Package:com.qingcheng.service.goods
 * Description:
 *
 * @Date:2020/6/19 22:07
 * @Author:jiaqi@163.com
 */
public interface StockBackService {

    //生成订单失败后，从rabbitmq接受消息orderItemList，插入库存回滚表，准备库存回滚
    public void addList(List<OrderItem> orderItemList);

    //执行库存回滚
    public void doBack();
}
