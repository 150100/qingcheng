package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.util.IdWorker;
import org.apache.zookeeper.ZooDefs;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderConfigMapper orderConfigMapper;
    @Autowired
    private OrderLogMapper orderLogMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired  //同一个模块调用service，直接用@Autowired
    private CartService cartService;
    @Reference
    private SkuService skuService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     * 在订单列表根据选中的ID查询未发货订单
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增。点击“提交订单”，增加订单
     * @param order
     */
    public Map<String,Object> add(Order order) {

        //1.获取选中的订单项
        List<Map<String, Object>> cartList = cartService.findNewOrderItemList(order.getUsername()); //获取最新的购物车列表
        List<OrderItem> orderItemList = cartList.stream().filter(cart -> (boolean) cart.get("checked") == true)
                .map(cart -> (OrderItem) cart.get("item")).collect(Collectors.toList());

        //2.减少库存，增加销量
        if (!skuService.deductionStock(orderItemList)) {
            new RuntimeException("库存不足！");
        }

        try {
            //3.保存订单表
            order.setId(idWorker.nextId()+"");
            //分组查询
            IntStream numStream = orderItemList.stream().mapToInt(OrderItem::getNum);
            IntStream moneyStream = orderItemList.stream().mapToInt(OrderItem::getMoney);
            int totalNum = numStream.sum(); //总数量
            int totalMoney = moneyStream.sum(); //总金额
            order.setTotalNum(totalNum);
            order.setTotalMoney(totalMoney);
            order.setCreateTime(new Date());
            order.setOrderStatus("0"); //未付款
            order.setPayStatus("0");//未支付
            order.setConsignStatus("0");//未发货
            int preMoney = cartService.preferential(order.getUsername());//满减优惠金额
            order.setPreMoney(preMoney);
            order.setPayMoney(totalMoney-preMoney);//实付金额

            orderMapper.insert(order);

            //4.保存订单项表
            double proportion = (double) preMoney / totalMoney; //打折比例
            for (OrderItem orderItem : orderItemList) {
                orderItem.setId(idWorker.nextId()+"");
                orderItem.setOrderId(order.getId());
                orderItem.setPayMoney((int) (orderItem.getMoney()*proportion));//作为将来退款用

                orderItemMapper.insert(orderItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //发送回滚消息
            rabbitTemplate.convertAndSend("","queue.skuback", JSON.toJSONString(orderItemList)); //参数：交换器，队列，发送内容
            throw new RuntimeException("生成订单失败！");//抛出异常，回滚try语句块
        }

        //5.删除购物车勾选商品
        cartService.deleteCheckedCart(order.getUsername());
        Map map = new HashMap<>();
        map.put("ordersn", order.getId()); //订单号
        map.put("money", order.getPayMoney()); //实付金额

        //向queue.ordercreate发送消息。自己写的(超时未支付作业)
        rabbitTemplate.convertAndSend("", "queue.ordercreate", order.getId());

        return map;
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    //根据订单id，返回订单组合实体类OrderOrderItem给前端
    public OrderOrderItem findOrderOrderItem(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId", id);
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);

        OrderOrderItem orderOrderItem = new OrderOrderItem();
        orderOrderItem.setOrder(order);
        orderOrderItem.setOrderItemList(orderItems);
        return orderOrderItem;
    }

    @Override
    //批量发货
    public void batchSend(List<Order> orders) {
        for (Order order : orders) {
            if (order.getShippingCode() == null || order.getShippingName() == null) {
                throw new RuntimeException("请选择快递公司和填写快递单号");
                /*为什么要遍历两次呢？（这里抛出异常后，公共异常里面有return，整个方法batchSend就终止了）现在目的是要么订单全成功，
                要么全失败，如果只遍历一次，把下面的合并过来，会导致比如5个订单，前两个成功，第三个空，失败，后面俩也不执行了*/
            }
        }
        for (Order order : orders) {
            order.setOrderStatus("3"); //订单状态，已完成
            order.setConsignStatus("2"); //发货状态，已发货
            order.setConsignTime(new Date()); //发货时间
            orderMapper.updateByPrimaryKeySelective(order);

            //记录订单日志，自己写
        }
    }

    @Override
    //订单超时处理逻辑
    public void orderTimeOutLogic() {
        //查询id为1的OrderConfig记录
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        //查询到了本记录超时时间为60分
        Integer orderTimeout = orderConfig.getOrderTimeout();
        //从现在时间往前推xx时间，得到时间点。本次是往前推60分钟
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);

        //查询满足条件的订单
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime", localDateTime); //查询订单创建时间小于超时时间点的
        criteria.andEqualTo("orderStatus", 0); //未付款的
        criteria.andEqualTo("isDelete", "0"); //未删除的
        List<Order> orders = orderMapper.selectByExample(example);
        for (Order order : orders) {
            //设置订单日志OrderLog
            OrderLog orderLog = new OrderLog();
            orderLog.setOperater("system"); //操作员
            orderLog.setOperateTime(new Date()); //当前日期
            orderLog.setOrderStatus("4"); //已关闭
            orderLog.setPayStatus(order.getPayStatus()); //支付状态
            orderLog.setConsignStatus(order.getConsignStatus()); //发货状态
            orderLog.setRemarks("订单超时，已关闭！");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insert(orderLog);

            //更新Order
            order.setOrderStatus("4"); //订单关闭
            order.setCloseTime(new Date()); //关闭日期
            orderMapper.updateByPrimaryKeySelective(order);
        }
    }

    @Override
    //合并订单（自己写的）
    public Order merge(String orderId1, String orderId2) {
        //orderId2信息合并到orderId1
        Order order1 = orderMapper.selectByPrimaryKey(orderId1);
        Order order2 = orderMapper.selectByPrimaryKey(orderId2);
        order1.setTotalNum(order1.getTotalNum()+order2.getTotalNum());
        order1.setTotalMoney(order1.getTotalMoney()+order2.getTotalMoney()-order2.getPostFee());
        order1.setPayMoney(order1.getPayMoney()+order2.getPayMoney()-order2.getPostFee());
        order1.setUpdateTime(new Date());
        //orderId2订单项合并到orderId1
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId", order2);
        List<OrderItem> orderItems = orderItemMapper.selectByExample(example);
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(orderId1);
        }
        orderMapper.updateByPrimaryKeySelective(order1);
        //对订单2逻辑删除
        order2.setIsDelete("1");
        //订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOperater("system"); //操作员
        orderLog.setOperateTime(new Date()); //当前日期
        orderLog.setOrderStatus("0"); //待付款
        orderLog.setPayStatus(order1.getPayStatus()); //支付状态
        orderLog.setConsignStatus(order1.getConsignStatus()); //发货状态
        orderLog.setRemarks("两个订单已合并！");
        orderLog.setOrderId(order1.getId());
        orderLogMapper.insert(orderLog);

        return order1;
    }

    /*@Override
    //拆分订单（自己写的，不会）。传过来的参数[{id:1,num:10},{id:2,num:5}...]，要拆分的是订单项，所以id是订单项的
    public List<Order> split(List<OrderItem> orderItems) {

    }*/


    @Override
    //修改订单状态和订单日志 （俩参数：订单号和流水号）
    public void updatePayStatus(String orderId, String transactionId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order != null && "0".equals(order.getPayStatus())) {
            order.setPayStatus("1");
            order.setOrderStatus("1");
            order.setUpdateTime(new Date());
            order.setPayTime(new Date());
            order.setTransactionId(transactionId);//流水号
            orderMapper.updateByPrimaryKeySelective(order);

            //记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId() + "");
            orderLog.setOrderId(order.getId());
            orderLog.setPayStatus("1");
            orderLog.setOrderStatus("1");
            orderLog.setOperateTime(new Date());
            orderLog.setOperater("system"); //操作人为系统
            orderLog.setRemarks("支付流水号：" + transactionId);//备注
            orderLogMapper.insert(orderLog);
        }
    }




    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }
            //根据id数组查询
            if (searchMap.get("ids") != null) {
                criteria.andIn("id", Arrays.asList((String[]) searchMap.get("ids")));
            }

        }
        return example;
    }

}
