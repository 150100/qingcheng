package com.qingcheng.pojo.order;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName:OrderOrderItem
 * Package:com.qingcheng.pojo.order
 * Description:
 *
 * @Date:2020/3/21 16:37
 * @Author:jiaqi@163.com
 */
public class OrderOrderItem implements Serializable {

    private Order order;
    private List<OrderItem> orderItemList;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
