package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SpuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName:ItemController
 * Package:com.qingcheng.controller
 * Description:  生成商品详情页面
 *
 * @Date:2020/4/25 18:10
 * @Author:jiaqi@163.com
 */
@RestController
@RequestMapping("/item")
public class ItemController {

    @Reference
    private SpuService spuService;
    @Value("${pagePath}")
    private String pagePath;
    @Reference
    private TemplateEngine templateEngine;
    @Reference
    private CategoryService categoryService;

    @GetMapping("/createPage")
    public void createPage(String spuId) {
        //1.查询商品信息
        Goods good = spuService.findGoodById(spuId);
        Spu spu = good.getSpu();
        List<Sku> skuList = good.getSkuList();
        //查询商品分类
        List<String> categoryList = new ArrayList<>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName()); //一级分类名称
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName()); //二级分类名称
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName()); //三级分类名称

        //把spu下所有sku规格组合的url放在一个Map里
        Map<String, String> urlMap = new HashMap<>();
        for (Sku sku : skuList) {
            if ("1".equals(sku.getStatus())){
                //下面这行会对Map的key进行排序
                String specJson = JSON.toJSONString(JSON.parseObject(sku.getSpec()), SerializerFeature.MapSortField);
                urlMap.put(specJson, sku.getId() + ".html"); //这个Map举例：【颜色：黑色，内存：64G】对应的url是....html
            }
        }

        //2.批量生成sku页面
        for (Sku sku : skuList) {
            //(1)创建上下文和数据模型
            Context context = new Context();

            Map<String,Object> dataModel = new HashMap<>();
            dataModel.put("spu", spu);
            dataModel.put("sku", sku);
            dataModel.put("categoryList", categoryList);
            dataModel.put("skuImages", sku.getImages().split(",")); //sku图片列表(特有的图片)，分割成数组！
            dataModel.put("spuImages", spu.getImages().split(",")); //spu图片列表(公共的图片)，分割成数组！

            Map paramItems = JSON.parseObject(spu.getParaItems()); //参数列表
            dataModel.put("paramItems", paramItems);

            Map<String,String> specItems = (Map)JSON.parseObject(sku.getSpec()); //规格列表
            dataModel.put("specItems", specItems);

            //{"颜色":["黑色","白色","红色"],"版本":["6GB+64GB"]} 转换成下面这行：
            //{"颜色":[{'option':'黑色',checked:true,url:(黑色+64G的url)...},{'option':'白色',checked:false,url:(白色+64G的url)...}...]....}
            Map<String,List> specMap = (Map)JSON.parseObject(spu.getSpecItems());//规格和规格选项，包含了所有sku的规格
            for (String key : specMap.keySet()) {  //颜色,版本...
                List<String> list = specMap.get(key);   //["黑色","白色","红色"]or["6GB+64GB"]...
                List<Map> mapList = new ArrayList<>();  //新的集合：[{'option':'黑色',checked:true},{'option':'白色',checked:false}]
                for (String value : list) {   //黑色
                    Map map = new HashMap();
                    map.put("option", value);
                    if (specItems.get(key).equals(value)) {  //如果和当前sku的规格值相同，就选中
                        map.put("checked", true); //是否选中
                    } else {
                        map.put("checked", false);
                    }
                    //获取当前sku规格例如： "颜色：黑色，内存：64G" ，转化为Map
                    Map<String, String> spec = (Map)JSON.parseObject(sku.getSpec()); //当前sku的规格
                    //用当前遍历的key，value进行覆盖,举例：用【颜色：黑色】覆盖了当前sku，但是只覆盖了颜色，内存还是原来的，以此类推，排列组合
                    spec.put(key, value);
                    //对key进行排序
                    String specJson = JSON.toJSONString(spec, SerializerFeature.MapSortField);
                    map.put("url", urlMap.get(specJson));

                    mapList.add(map);
                }
                specMap.put(key, mapList); //用新集合替换原有集合
            }
            dataModel.put("specMap", specMap);

            context.setVariables(dataModel);
            //（2）准备文件
            File dir = new File(pagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File dest = new File(dir, sku.getId() + ".html"); //文件路径和名称
            //（3）生成页面
            try {
                PrintWriter writer = new PrintWriter(dest,"UTF-8");
                templateEngine.process("item",context,writer); //根据哪个模板生成、上下文、输出对象
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }
}
