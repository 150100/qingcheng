package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * ClassName:SearchController
 * Package:com.qingcheng.controller
 * Description:
 *
 * @Date:2020/5/13 10:17
 * @Author:jiaqi@163.com
 */
@Controller
public class SearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    public String search(Model model, @RequestParam Map<String, String> searchMap) throws Exception {

        //字符集处理（中文搜索）
        searchMap = WebUtil.convertCharsetToUTF8(searchMap);

//        如果前端没有传过来页码，默认是1
        if (searchMap.get("pageNo") == null) {
            searchMap.put("pageNo", "1");
        }
//        页面传给后端两个参数： sort：排序字段，sortOrder：排序规则（升序/降序）
        if (searchMap.get("sort") == null) {
            searchMap.put("sort", "");
        }
        if (searchMap.get("sortOrder") == null) {
            searchMap.put("sortOrder", "DESC");
        }


        Map result = skuSearchService.search(searchMap);
        model.addAttribute("result", result);

        //url拼接处理
        StringBuffer url = new StringBuffer("/search.do?");
        for (String key : searchMap.keySet()) {
            url.append("&" + key + "=" + searchMap.get(key));
        }
        model.addAttribute("url", url);

        model.addAttribute("searchMap", searchMap);

//        当前页码字符串转为int，供前端使用，因为要判断是否和${page}相等，必须转化int
        int pageNo = Integer.parseInt(searchMap.get("pageNo"));
        model.addAttribute("pageNo", pageNo);

//        在页码显示的地方只显示5个页码
        Long totalPages = (Long) result.get("totalPages"); //获取总页数
        int startPage = 1; //开始页码
        int endPage = totalPages.intValue(); //截止页码。Long转化为int
        if (totalPages > 5) {
            startPage = pageNo-2;
            if (startPage < 1) {
                startPage = 1;
            }
            endPage = startPage + 4;
            if (endPage > totalPages) { //这个if判断是自己写的，感觉原项目缺失
                endPage = totalPages.intValue();
            }
        }
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);


        return "search";
    }
}
