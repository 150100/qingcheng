package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.OrderOrderItem;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);

//    点击“提交订单”，增加订单
    public Map<String,Object> add(Order order);


    public void update(Order order);


    public void delete(String id);

    public OrderOrderItem findOrderOrderItem(String id);

    public void batchSend(List<Order> orders);

    public void orderTimeOutLogic();

    public Order merge(String orderId1, String orderId2);

//    拆分订单（自己写的，不会）。传过来的参数[{id:1,num:10},{id:2,num:5}...]，要拆分的是订单项，所以id是订单项的
//    public List<Order> split(List<OrderItem> orderItems);

//  修改订单状态
    public void updatePayStatus(String orderId, String transactionId);//transactionId：流水号

}
