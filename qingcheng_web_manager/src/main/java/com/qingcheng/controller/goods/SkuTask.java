package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ClassName:SkuTask
 * Package:com.qingcheng.controller.goods
 * Description:
 *
 * @Date:2020/6/20 12:21
 * @Author:jiaqi@163.com
 */
@Component
public class SkuTask {

    @Reference
    private StockBackService stockBackService;

    //间隔一小时，执行一次库存回滚
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void skuStockBack() {
        stockBackService.doBack();
    }
}
