package com.qingcheng.service.impl;

import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    private AdService adService;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("缓存预热----------------------");
        adService.saveAllAdToRedis();
    }
}
