package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName:SkuController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/5/7 9:49
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/sku")
@CrossOrigin
public class SkuController {

    @Reference
    private SkuService skuService;

    @GetMapping("/price")
    public Integer price(String id) {
        return skuService.findPrice(id);
    }


}
