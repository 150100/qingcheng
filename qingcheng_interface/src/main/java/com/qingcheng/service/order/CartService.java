package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

/**
 * ClassName:CartService
 * Package:com.qingcheng.service.order
 * Description:
 *
 * @Date:2020/5/31 11:05
 * @Author:jiaqi@163.com
 */
//购物车服务
public interface CartService {
//    从redis获取某用户购物车列表
    public List<Map<String, Object>> findCartList(String username);

    //  向购物车添加商品（用户名，商品id，数量）
    public void addItem(String username, String skuId, Integer num);

    //保存勾选状态
    public boolean updateChecked(String username, String skuId, boolean checked);

    //删除勾选商品
    public void deleteCheckedCart(String username);

    //计算购物车的优惠金额
    public int preferential(String username);

    //在购物车页面点击结算进入结算页面后，获取最新的购物车列表
    public List<Map<String, Object>> findNewOrderItemList(String username);
}
