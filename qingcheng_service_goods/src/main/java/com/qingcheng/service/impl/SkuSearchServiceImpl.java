package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.service.goods.SkuSearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName:SkuSearchServiceImpl
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/5/13 8:33
 * @Author:jiaqi@163.com
 */
@Service
//通过elasticsearch来进行搜索
public class SkuSearchServiceImpl implements SkuSearchService {

    //1.连接rest接口已经配到spring的bean中

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private SpecMapper specMapper;

    @Override
    public Map search(Map<String, String> searchMap) {

        //2.封装查询请求
        SearchRequest searchRequest = new SearchRequest("sku");
        searchRequest.types("doc"); //设置查询类型

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); //查询源构建器

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();  //布尔查询构建器

        //2.1 关键字查询
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);

        //2.2 分类 过滤查询。name属性是在es里面创建时起的（也就是本例中的categoryName，品牌也一样）
        if (searchMap.get("category") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("category"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //2.3 品牌 过滤查询
        if (searchMap.get("brand") != null) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brand"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //2.4 规格 过滤查询。规格和上面的不一样，categoryName、brandName的type定义为keyword（不分词），而spec定义为object，因为本身是个对象
        //在kibana查询的时候用的是 spec.颜色.keyword，才能得出结果，所以这里的key实际上是 spec.颜色
        for (String key : searchMap.keySet()) {
            if (key.startsWith("spec")) {
                TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key+"keyword", searchMap.get(key));
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //2.5 价格 过滤查询
        if (searchMap.get("price") != null) {
            String[] price = searchMap.get("price").split("-");
            if (!"0".equals(price[0])) {    //如果最低价格不为0
                //gte是>= 。因为单位是元，这里转化成分去查。在es这种查询中，数值和字符串可以自动转化
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(price[0] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            if (!"*".equals(price[1])) {    //如果最高价格不为*，也就是无上限
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(price[1] + "00");  //lte <=
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
        }

        //2.6 分页（根据当前页码和页大小来获取当前分页数据）
        Integer pageNo = Integer.parseInt(searchMap.get("pageNo")); //当前页码
        Integer pageSize = 30; //页大小（多少条数据）
        int fromIndex = (pageNo - 1) * pageSize; //根据页码得出第一条记录的索引（固定公式）
        searchSourceBuilder.from(fromIndex); //开始索引
        searchSourceBuilder.size(pageSize); //每页大小

        searchSourceBuilder.query(boolQueryBuilder);

        //2.7 搜索结果排序：
        String sort = searchMap.get("sort");    //排序字段
        String sortOrder = searchMap.get("sortOrder");  //排序规则（升序/降序）
        if (!"".equals(sort)) {
            searchSourceBuilder.sort(sort, SortOrder.valueOf(sortOrder));
        }

//        高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name").preTags("<font style='color:red'>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);

        //分组查询（商品分类）。比如说关键字搜了个“小米”，查找name包含小米的所有分类
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //3.封装查询响应。  能看出来只有3.1和3.2跟elasticsearch有关，后面的实际已经根据分类从数据库查找了
        Map resultMap = new HashMap<>();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = searchResponse.getHits();
            long totalHits = searchHits.getTotalHits(); //总记录数
            System.out.println(totalHits);
            SearchHit[] hits = searchHits.getHits();

            //3.1 搜索出的商品列表
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (SearchHit hit : hits) {
                Map<String, Object> skuMap = hit.getSourceAsMap();

//                name高亮设置
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField name = highlightFields.get("name");
                Text[] fragments = name.fragments();
                skuMap.put("name", fragments[0].toString()); //用高亮结果替换原来的

                resultList.add(skuMap);
            }
            resultMap.put("rows", resultList);

            //3.2 分类列表（筛选面板）。根据关键字查询到字段categoryName对应的分类名
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
            Terms terms = (Terms) aggregationMap.get("sku_category");
            List<? extends Terms.Bucket> buckets = terms.getBuckets();
            List<String> categoryList = new ArrayList<>();
            for (Terms.Bucket bucket : buckets) {
                categoryList.add(bucket.getKeyAsString());  //这里直接就是分类名称了
            }
            resultMap.put("categoryList", categoryList);



            String categoryName=""; //商品分类名称
            if (searchMap.get("category") == null) { //如果没有分类条件（就是用户没点筛选面板的分类选项）
                if (categoryList.size() > 0) {
                    categoryName = categoryList.get(0); //取第一个，显示出来
                }
            } else {
                categoryName = searchMap.get("category");//否则就是有分类条件，取出来赋给categoryName
            }
            //3.3 品牌列表（筛选面板）
            if (searchMap.get("brand") == null) {
                List<Map> brandList = brandMapper.findBrandByCategoryName(categoryName); //通过分类查找品牌列表
                resultMap.put("brandList", brandList);
                /*这里返回的brandList里面的brand数据，不是brand名称，而是查询结果brand里面的name和image，所以前端页面需要写brand.name*/
            }

            //3.4 规格列表（筛选面板）
            List<Map> specList = specMapper.findSpecByCategoryName(categoryName);
            for (Map spec : specList) {
                String[] options = ((String) spec.get("options")).split(","); //将字符串按逗号分割成数组
                spec.put("options", options);   //重新装回去
            }
            resultMap.put("specList", specList);

            //3.5 总页数=总记录数/页大小
            long pageCount = (totalHits % pageSize == 0) ? (totalHits / pageSize) : (totalHits / pageSize + 1);
            resultMap.put("totalPages", pageCount);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultMap;
    }
}
