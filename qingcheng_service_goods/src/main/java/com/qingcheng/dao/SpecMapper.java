package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Spec;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SpecMapper extends Mapper<Spec> {

//    根据分类查规格列表
    @Select("select name,options from tb_spec where template_id in " +
            "(select  template_id from tb_category where name=#{name} ) order by seq")
    public List<Map> findSpecByCategoryName(@Param("name") String categoryName);
}

/*查询结果：
name        options
*颜色	红色,蓝色,黑色,槟色,白色,金色,银色,灰色.紫色
版本	    8GB+128GB,6GB+64GB,6GB+128GB,4GB+64GB,4GB+32GB
我编的	1,2,3,4,5,6

* */