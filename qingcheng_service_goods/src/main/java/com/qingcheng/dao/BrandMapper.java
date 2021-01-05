package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import javax.swing.plaf.PanelUI;
import java.util.List;
import java.util.Map;

public interface BrandMapper extends Mapper<Brand> {

//    根据分类查品牌列表
    @Select("select name,image from tb_brand where id in( " +
            "select brand_id from tb_category_brand where category_id in( " +
            "select id from tb_category where name=#{name} )) order by seq")
    public List<Map> findBrandByCategoryName(@Param("name") String categoryName);
}
/*查询结果：
*   name            image
* 三星	    http://img14.360buyimg.com/popshop/jfs/t2701/34/484677369/7439/ee13e8fa/5716e2c4Nc925baf3.jpg
  康佳
  TCL
  长虹
  LG

* */