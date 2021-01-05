package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.SpecService;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * ClassName:OrderTask
 * Package:com.qingcheng.controller.order
 * Description:
 *
 * @Date:2020/3/26 11:44
 * @Author:jiaqi@163.com
 */
@Component
public class OrderTask {
    @Reference
    private OrderService orderService;
    @Reference
    private CategoryReportService categoryReportService;
    @Reference
    private BrandService brandService;
    @Reference
    private SpecService specService;

    @Scheduled(cron = "0 0/2 * * * ?")  //秒 分 时 日 月 周几
    public void orderTimeOutLogic() {
        System.out.println("每隔2分钟执行一次任务" + new Date());
        orderService.orderTimeOutLogic();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void createData() {
        System.out.println("每天凌晨1点生成类目统计数据");
        categoryReportService.createData();
    }

//    每天凌晨一点将所有根据分类名查询品牌列表数据放入缓存。 自己写的
    @Scheduled(cron = "0 0 1 * * ?")
    public void saveAllBrandToRedisByCategory() {
        brandService.saveAllBrandToRedisByCategory();
    }
    //    每天凌晨一点将所有根据分类名查询规格列表数据放入缓存。 自己写的
    @Scheduled(cron = "0 0 1 * * ?")
    public void saveAllSpecToRedisByCategory() {
        specService.saveAllSpecToRedisByCategory();
    }
}
