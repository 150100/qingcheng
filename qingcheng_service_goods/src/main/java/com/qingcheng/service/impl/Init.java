package com.qingcheng.service.impl;

import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpecService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * ClassName:Init
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/5/5 17:08
 * @Author:jiaqi@163.com
 */
@Component
//实现该接口方法后，启动会自动调用，然后实现缓存预热
public class Init implements InitializingBean {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SkuService skuService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SpecService specService;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("缓存预热----------------------");
        categoryService.saveCategoryTreeToRedis();  //加载商品分类树缓存
        skuService.saveAllPriceToRedis();   //加载价格数据

        //如果CATEGORY_BRAND为空，根据所有分类查询对应品牌列表，并放入缓存
        if (redisTemplate.boundHashOps(CacheKey.CATEGORY_BRAND).size() == 0) {
            brandService.saveAllBrandToRedisByCategory();
        }

        //如果CATEGORY_SPEC为空，根据所有分类查询对应规格列表，并放入缓存
        if (redisTemplate.boundHashOps(CacheKey.CATEGORY_SPEC).size() == 0) {
            specService.saveAllSpecToRedisByCategory();
        }
    }
}
