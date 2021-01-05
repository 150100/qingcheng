package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.StockBackMapper;
import com.qingcheng.pojo.goods.StockBack;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * ClassName:StockBackServiceImpl
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/6/19 22:46
 * @Author:jiaqi@163.com
 */
@Service(interfaceClass = StockBackService.class)
//库存回滚业务
public class StockBackServiceImpl implements StockBackService {

    @Autowired
    private StockBackMapper stockBackMapper;
    @Autowired
    private SkuMapper skuMapper;

    @Override
    @Transactional
    //生成订单失败后，从rabbitmq接受消息orderItemList，插入库存回滚表，准备库存回滚
    public void addList(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            StockBack stockBack = new StockBack();
            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setStatus("0");
            stockBack.setNum(orderItem.getNum());
            stockBack.setCreateTime(new Date());

            //插入库存回滚表
            stockBackMapper.insert(stockBack);
        }

    }

    @Override
    //执行库存回滚
    public void doBack() {
        System.out.println("库存回滚任务开始");
        //查询库存回滚表中状态为0的记录
        StockBack stockBack0 = new StockBack();
        stockBack0.setStatus("0"); //未回滚
        List<StockBack> stockBackList = stockBackMapper.select(stockBack0);
        for (StockBack stockBack : stockBackList) {
            skuMapper.deductionStock(-stockBack.getNum(),stockBack.getSkuId());//因为要增加库存，所以数量前加负号
            skuMapper.addSaleNum(-stockBack.getNum(),stockBack.getSkuId());//同理，减少销量
            stockBack.setStatus("1");
            stockBackMapper.updateByPrimaryKey(stockBack);
        }
        System.out.println("库存回滚任务结束");
    }
}
