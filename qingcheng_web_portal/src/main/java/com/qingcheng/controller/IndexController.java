package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * ClassName:IndexController
 * Package:com.qingcheng.controller
 * Description: 主页
 *
 * @Date:2020/4/20 15:44
 * @Author:jiaqi@163.com
 */
@Controller
public class IndexController {

    @Reference
    private AdService adService;
    @Reference
    private CategoryService categoryService;

    @GetMapping("/index")
    public String index(Model model) {
        //得到首页广告轮播图列表
        List<Ad> lbtList = adService.findByPosition("index_lb");
        model.addAttribute("lbt", lbtList);

        //得到分类列表（分类树）
        List<Map> categoryList = categoryService.findCategoryTree();
        model.addAttribute("categoryList", categoryList);

        return "index";

    }

}
