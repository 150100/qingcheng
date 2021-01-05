package com.qingcheng.dao;

import com.qingcheng.pojo.order.CategoryReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ClassName:CategoryReportMapper
 * Package:com.qingcheng.dao
 * Description:
 *
 * @Date:2020/3/28 18:11
 * @Author:jiaqi@163.com
 */
public interface CategoryReportMapper extends Mapper<CategoryReport> {

    //根据给定的一个日期查询三个分类类目统计
    //自定义查询注解。通用Mapper自定义方法，这些字段要起别名，因为自定义不会进行驼峰转化了
    @Select("SELECT category_id1 categoryId1, category_id2 categoryId2, category_id3 categoryId3, DATE_FORMAT(o.pay_time,'%Y‐%m‐%d') countate, SUM(oi.num) num, SUM(oi.pay_money) money " +
            "FROM tb_order_item oi, tb_order o " +
            "WHERE oi.ordeDr_id=o.id AND o.pay_status='1' AND DATE_FORMAT(o.pay_time,'%Y‐%m‐%d')=#{date} " +
            "GROUPBY category_id1, category_id2, category_id3, DATE_FORMAT(o.pay_time,'%Y‐%m‐%d')")
    public List<CategoryReport> categoryReport(@Param("date") LocalDate date); //这个"date"要和上面查询语句的#{date}相同

    //日期区间查询categoryId1类目统计。v_category1是创造出来的视图
    @Select("SELECT category_id1 categoryId1, c.name categoryName, SUM(num) num, SUM(money) money " +
            "FROM tb_category_report r , v_category1 c " +
            "WHERE r.category_id1=c.id AND count_date>=#{date1} AND count_date<=#{date2} " +
            "GROUP BY category_id1, c.name")
    public List<Map> category1Count(@Param("date1")String date1, @Param("date2")String date2); //返回类型的泛型用CategoryReport也行
}
