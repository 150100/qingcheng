package com.qingcheng.util;

/**
 * ClassName:CacheKey
 * Package:com.qingcheng.util
 * Description: 所有缓存key统一管理
 *
 * @Date:2020/5/5 12:45
 * @Author:jiaqi@163.com
 */
public enum CacheKey {
    AD, //广告
    SKU_PRICE,  //价格
    CATEGORY_TREE,  //分类树
    CATEGORY_BRAND, //根据分类名获取品牌列表（自己写的）
    CATEGORY_SPEC, //根据分类名获取规格列表（自己写的）
    CATEGORY, //分类
    CART_LIST, //购物车列表
}
