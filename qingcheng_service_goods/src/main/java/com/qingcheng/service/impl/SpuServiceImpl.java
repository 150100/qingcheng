package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.mysql.jdbc.StringUtils;
import com.qingcheng.dao.CategoryBrandMapper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.SpuMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private CategoryBrandMapper categoryBrandMapper;
    @Autowired
    private SkuService skuService;

    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  物理删除
     * @param id
     */
    public void delete(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu.getIsDelete()=="1") {
            spuMapper.deleteByPrimaryKey(id);
        }
    }

//    修改和新增商品共用同一个方法，需要做判断
    @Override
    @Transactional
    public void saveGoods(Goods goods) {

        //保存一个spu信息
        Spu spu = goods.getSpu();
        if (spu.getId() == null) { //为空则是新增
            //雪花算法生成id。因为idWorker.nextId()是long类型，所以加个空串变成字符串
            spu.setId(idWorker.nextId() + "");
            spuMapper.insert(spu);
        } else { //否则是修改
            //先删除sku列表，根据条件删除
            Example example = new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId", spu.getId());
            skuMapper.deleteByExample(example);
            //执行spu的修改
            spuMapper.updateByPrimaryKeySelective(spu);
        }


        Date date = new Date();
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id()); //通过分类3ID获取对应category
        //保存sku列表的信息
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {
            if (sku.getId() == null) { //为空说明是新增
            sku.setId(idWorker.nextId()+"");
            sku.setCreateTime(date); //创建日期
            }
            sku.setSpuId(spu.getId());

            if (sku.getSpec()==null || "".equals(sku.getSpec())) {
                sku.setSpec("{}");
            }

            //sku名称=spu名称+spec字段
            String name = spu.getName();
            //因为sku.getSpec()为字符串，需要转换为Map。sku.getSpec()举例：{'颜色': '红色', '版本': '8GB+128GB'}
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for (String value : specMap.values()) {
                name += " " + value; //最终结果类似于：华为 HUAWEI 麦芒7 6G+64G 亮黑色 全网通  前置智慧双摄  移动联通电信4G手机 双卡双待
            }
            sku.setName(name); //名称
            sku.setUpdateTime(date); //更新日期
            sku.setCategoryId(spu.getCategory3Id()); //分类id。括号里也可写 category.getId();
            sku.setCategoryName(category.getName()); //分类名称
            sku.setCommentNum(0); //评论数
            sku.setSaleNum(0); //销量

            skuMapper.insert(sku);

            //更新价格缓存
            skuService.savePriceToRedisById(sku.getId(),sku.getPrice());

        }

        //建立分类和品牌关联
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setCategoryId(spu.getCategory3Id());
        categoryBrand.setBrandId(spu.getBrandId());
        //如果没有以下代码，本方法由于加了事务注解，会导致一有重复数据，所有操作都会被撤销
        int i = categoryBrandMapper.selectCount(categoryBrand);
        if (i == 0) {
            categoryBrandMapper.insert(categoryBrand);
        }

    }

    @Override
    public Goods findGoodById(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);

        //查询sku列表
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId", id);
        List<Sku> skuList = skuMapper.selectByExample(example);

        //封装为组合实体类 (放进Goods)
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);

        return goods;
    }

    @Override
    //商品审核
    public void audit(String id, String status, String message) {
        //1、修改审核状态和上架状态
        Spu spu = new Spu(); //直接创建比查询后修改效率高
        spu.setId(id);
        spu.setStatus(status);
        if ("1".equals(status)) { //审核通过
            spu.setIsMarketable("1"); //自动上架
        }
        spuMapper.updateByPrimaryKeySelective(spu);

        //2、记录商品审核

        //3、记录商品日志
    }

    @Override
    public void pull(String id) {
        //1、商品下架
        Spu spu = new Spu();
        spu.setIsMarketable("0");
        spuMapper.updateByPrimaryKeySelective(spu);

        //2、记录商品日志
    }

    @Override
    public void put(String id) {
        //1、商品上架
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (!"1".equals(spu.getStatus())) {
            throw new RuntimeException("此商品未通过审核");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    //批量上架
    public int putMany(String[] ids) {
        //1、修改状态
        Spu spu = new Spu();
        spu.setIsMarketable("1");

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isMarketable", "0"); //下架的
        criteria.andEqualTo("status", "1"); //审核通过的
        return spuMapper.updateByExampleSelective(spu, example); //按照example查出来的都更新成刚创建的spu.setIsMarketable("1");

        //2、添加商品日志

    }

    @Override
    //批量下架
    public int pullMany(String[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("0");

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isMarketable", "1"); //上架的
        return spuMapper.updateByExampleSelective(spu, example);
    }

    @Override
    //逻辑删除商品，自己写的
    public void isdelete(String id) {
//      删除缓存中价格
        Map map = new HashMap();
        map.put("spuId", id);
        List<Sku> skuList = skuService.findList(map);
        for (Sku sku : skuList) {
            skuService.deletePriceFromRedis(sku.getId());
        }

        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setIsDelete("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    //恢复商品，自己写的
    public void recover(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setIsDelete("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id", searchMap.get("id"));
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
