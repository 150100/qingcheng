package com.qingcheng.pojo.goods;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * ClassName:StockBack
 * Package:com.qingcheng.pojo.goods
 * Description:
 *
 * @Date:2020/6/19 22:02
 * @Author:jiaqi@163.com
 */
@Table(name = "tb_stock_back")
public class StockBack {

    @Id
    private String orderId;

    @Id
    private String skuId;

    private Integer num; //回滚该库存数量

    private String status; //0：未回滚，1：已回滚

    private Date createTime; //创建时间

    private Date backTime; //回滚时间

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getBackTime() {
        return backTime;
    }

    public void setBackTime(Date backTime) {
        this.backTime = backTime;
    }
}
