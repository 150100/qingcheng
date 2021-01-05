package com.qingcheng.pojo.goods;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName:Goods
 * Package:com.qingcheng.pojo.goods
 * Description:
 *
 * @Date:2020/3/17 10:50
 * @Author:jiaqi@163.com
 */
public class Goods implements Serializable {

    private Spu spu;

    private List<Sku> skuList;

    public Spu getSpu() {
        return spu;
    }

    public void setSpu(Spu spu) {
        this.spu = spu;
    }

    public List<Sku> getSkuList() {
        return skuList;
    }

    public void setSkuList(List<Sku> skuList) {
        this.skuList = skuList;
    }
}
