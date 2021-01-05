package com.qingcheng.service.impl;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * ClassName:RestClientFactory
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/5/12 22:08
 * @Author:jiaqi@163.com
 */
//将es连接rest接口的步骤封装到bean里面交给spring管理
public class RestClientFactory {
    public static RestHighLevelClient getRestHighLevelClient(String hostName,int port) {
        //连接rest接口
        HttpHost http = new HttpHost(hostName, port, "http");
        RestClientBuilder restClientBuilder = RestClient.builder(http); //rest构建器
        return new RestHighLevelClient(restClientBuilder);  //高级客户端对象
    }
}
