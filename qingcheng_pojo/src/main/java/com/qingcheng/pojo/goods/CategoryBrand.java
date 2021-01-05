package com.qingcheng.pojo.goods;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ClassName:CategoryBrand
 * Package:com.qingcheng.pojo.goods
 * Description:
 *
 * @Date:2020/3/18 11:22
 * @Author:jiaqi@163.com
 */
@Table(name = "tb_category_brand")
public class CategoryBrand implements Serializable {
    @Id
    private Integer categoryId;
    @Id
    private Integer brandId;

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getBrandId() {
        return brandId;
    }

    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
    }
}
