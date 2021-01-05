package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ClassName:CategoryReportController
 * Package:com.qingcheng.controller.order
 * Description:
 *
 * @Date:2020/3/28 21:14
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/categoryReport")
public class CategoryReportController {
    @Reference
    private CategoryReportService categoryReportService;

    @GetMapping("/yesterday")
    public List<CategoryReport> yesterday() {
        LocalDate localDate = LocalDate.now().minusDays(1); //得到昨天日期
        return categoryReportService.categoryReport(localDate);
    }

    @GetMapping("/category1Count")
    public List<Map> category1Count(String date1, String date2) {
        return categoryReportService.category1Count(date1, date2);
    }
}
