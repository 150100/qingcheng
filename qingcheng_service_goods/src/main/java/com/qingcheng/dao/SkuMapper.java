package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {

    //减少库存
    @Select("update tb_sku set num=num-#{num} where id=#{id}")
    public void deductionStock(@Param("num") Integer num, @Param("id") String id);

    //增加销量
    @Select("update tb_sku set saleNum=saleNum+#{num} where id=#{id}")
    public void addSaleNum(@Param("num") Integer num, @Param("id") String id);
}
