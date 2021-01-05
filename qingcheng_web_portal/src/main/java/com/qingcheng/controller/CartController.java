package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.user.Address;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.user.AddressService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Repeatable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName:CartController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/5/31 11:45
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference
    private CartService cartService;
    @Reference
    private AddressService addressService;
    @Reference
    private OrderService orderService;


    @GetMapping("/findCartList")
    //获取购物车列表
    public List<Map<String, Object>> findCartList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> cartList = cartService.findCartList(username);
        return cartList;
    }

    @GetMapping("/addItem")
    //向购物车添加商品
    public Result addItem(String skuId, Integer num) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username, skuId, num);
        return new Result();
    }

    @GetMapping("/buy")
    //商品详情页点“加入购物车后”重定向到cart.html（跨域问题才不得不这么做）
    public void buy(HttpServletResponse response, String skuId, Integer num) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username, skuId, num);
        response.sendRedirect("/cart.html");
    }

    @GetMapping("/updateChecked")
    //保存勾选状态
    public Result updateChecked(String skuId, boolean checked) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.updateChecked(username, skuId, checked);
        return new Result();
    }

    @GetMapping("/deleteCheckedCart")
    //删除勾选商品
    public Result deleteCheckedCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.deleteCheckedCart(username);
        return new Result();
    }

    @GetMapping("/preferential")
    //计算购物车的优惠金额
    public Map preferential() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        int preferential = cartService.preferential(username);
        Map map = new HashMap();
        map.put("preferential", preferential);
        return map;
    }

    @GetMapping("/findNewOrderItemList")
    //在购物车页面点击结算进入结算页面后，获取最新的购物车列表
    public List<Map<String, Object>> findNewOrderItemList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return cartService.findNewOrderItemList(username);
    }

    @GetMapping("/findAddressList")
    //根据用户名查询地址列表
    public List<Address> findAddressList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return addressService.findByUsername(username);
    }

    //点击“提交订单”，增加订单
    @PostMapping("/saveOrder")
    public Map<String, Object> saveOrder(@RequestBody Order order) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setUsername(username);
        return orderService.add(order);
    }

}
