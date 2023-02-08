package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.constant.MqConst;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.checkerframework.checker.units.qual.C;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/6/10 14:45
 * 描述：
 **/
@Service
public class ManageServiceImpl implements ManageService {

    //  服务层调用mapper 层！
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        //  select * from base_category1;
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //  select * from base_category2 where category1_id=?
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id",category1Id));
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //  select * from base_category3 where category2_id=?
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id",category2Id));
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        //  本质：执行多表关联查询！ 使用xml 文件来解决、 回顾mybatis!
        return baseAttrInfoMapper.selectAttrInfoList(category1Id,category2Id,category3Id);
    }

    //  既能做保存，又能做修改！
    @Override
    @Transactional(rollbackFor = Exception.class) // 如果有异常则回滚数据 ,如果不写 rollbackFor = Exception.class ，只有发生运行时异常才会回滚！
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //  base_attr_info  base_attr_value

        //  修改的时候，baseAttrInfo.id 一定存在的！
        if (baseAttrInfo.getId()==null){
            //  执行完成插入数据之后， @TableId(type = IdType.AUTO) 表示插入完成之后，就可以获取到当前的主键Id
            baseAttrInfoMapper.insert(baseAttrInfo);
        } else {
            //  修改数据:
            this.baseAttrInfoMapper.updateById(baseAttrInfo);
            //  平台属性值修改： base_attr_value 无法确定用户到底要做 UPDATE ,DELETE ,INSERT 因此 ：先del 再 insert
            //  虽然调用的delete 方法。但是我们做的是逻辑删除！ 本质：是执行的update 操作，非 delete 操作！
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
        }

        //  获取到平台属性值集合数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //  int i = 1/0 ; 会发生回滚!
        //  保存：
        if (!CollectionUtils.isEmpty(attrValueList)){
            //  遍历数据保存到数据库表
            attrValueList.forEach((baseAttrValue )-> {
                //  base_attr_value.attr_id 页面提交的时候，不会携带这个字段数据.
                //  base_attr_value.attr_id = base_attr_info.id
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            });
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //  select * from base_attr_value where attr_id = ? and is_deleted = 0;
        return baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id",attrId));
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
        //  select * from base_attr_info where id = attrId and is_deleted = 0;
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //  赋值平台属性值集合数据
        if (baseAttrInfo!=null){
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        //  select * from spu_info where category3_id = 61 order by id desc limit 0,10;
        //  构建查询条件
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPage,spuInfoQueryWrapper);
    }

    @Override
    public IPage<BaseTrademark> getTradeMarkList(Page<BaseTrademark> baseTrademarkPage) {
        //  select * from base_trademark order by id desc limit 0,10;
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByDesc("id");
        return baseTrademarkMapper.selectPage(baseTrademarkPage,baseTrademarkQueryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        //  select  * from base_sale_attr where is_deleted = 0;
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //  调用mapper 层.
        return this.spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    public void onSale(Long skuId) {
        //  更新 update sku_info set is_sale = 1 where id = skuId;
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        this.skuInfoMapper.updateById(skuInfo);

        //发送消息：根据监听者来决定
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS , MqConst.ROUTING_GOODS_UPPER , skuId);
    }



    @Override
    public void cancelSale(Long skuId) {
        //  更新 update sku_info set is_sale = 0 where id = skuId;
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        this.skuInfoMapper.updateById(skuInfo);

        //发送消息：根据监听者来决定
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS , MqConst.ROUTING_GOODS_LOWER , skuId);
    }

    @Override
    public IPage getSkuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo) {
        //  select * from sku_info where category3_id = 61 order by id desc limit 0,10;
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("category3_id",skuInfo.getCategory3Id());
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,skuInfoQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void getSaveSkuInfo(SkuInfo skuInfo) {
        /*
            sku_info
            sku_image
            sku_attr_value
            sku_sale_attr_value
         */
        skuInfoMapper.insert(skuInfo);

        //  获取skuImage list 集合数据
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            //  循环遍历
            skuImageList.forEach(skuImage -> {
                //  赋值skuId
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        //  sku_attr_value 获取到平台属性集合数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            //  循环遍历
            skuAttrValueList.forEach(skuAttrValue -> {
                //  赋值skuId
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
        //  sku_sale_attr_value 获取到销售属性数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            //  循环遍历
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                //  赋值操作：
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId()); // 在保存数据的时候，将spuId 赋值到skuInfo
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }
        //  保存到布隆过滤器中
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        //  需要将skuId 添加进去
        bloomFilter.add(skuInfo.getId());

    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        //  select * from spu_image where spu_id = ? and is_delete = 0;
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id",spuId));
    }

    //  1. shift+f2     2.  ctrl + i
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
          实现,找出相关的表结构：
        spu_image
        spu_info
        spu_poster
        spu_sale_attr
        spu_sale_attr_value
         */
        spuInfoMapper.insert(spuInfo);
        //  先获取到spuImage list 集合
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        //  判断当前集合不为空，循环遍历插入
        if (!CollectionUtils.isEmpty(spuImageList)){
            spuImageList.forEach(spuImage -> {
                //  赋值spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }
        //  获取商品海报数据集合
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            //  判断集合是否为空
            spuPosterList.forEach(spuPoster -> {
                //  赋值spuId
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            });
        }

        //  获取商品的销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        //  判断是否有销售属性集合数据
        if (!CollectionUtils.isEmpty(spuSaleAttrList)){
            //  不为空，遍历
            spuSaleAttrList.forEach(spuSaleAttr -> {
                //  赋值spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);



                //  获取到销售属性值的集合数据
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                //  判断集合是否为空
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        //  赋值spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //  赋值销售属性名
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }
    }



    @Override
    /**
     * 根据skuId 获取最新的商品价格
     */
    public BigDecimal getSkuPrice(Long skuId) {

        String locKey = "price" + skuId;
        RLock lock = redissonClient.getLock(locKey);
        lock.lock();

        try {
            //select price from sku_info where id = skuId and is_delete = 0;
            QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
            //设置查询条件
            skuInfoQueryWrapper.eq("id" , skuId);
            //设置查询字段
            skuInfoQueryWrapper.select("price");
            SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoQueryWrapper);
            //判断是否存在
            if (skuInfo != null){
                //返回真实的商品价格
                return skuInfo.getPrice();
            }
            //默认返回
            return new BigDecimal("0");
        } finally {
            lock.unlock();
        }
    }

    //  skuInfo + skuImageList
    @Override
    @GmallCache(prefix = "SkuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {
        //  return getSkuInfoByRedisLock(skuId);
        //  return getSkuInfoByRedissonLock(skuId);
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoByRedisLock(Long skuId) {
        try {
            // return getSkuInfoByRedisLock(skuId）
            //定义缓存的key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // 获取缓存中的数据，看是否数据存在
            SkuInfo skuInfo =(SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //在判断是否存在
            if(skuInfo == null){
                // 说明缓存中没有数据
                // 定义分布式锁的key
                String lockey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
                // 上锁 redisson
                RLock lock = redissonClient.getLock(lockey);
                // 第一个参数最大等待时间，第二个参数才是锁的过期时间，第三个参数时间单位
                Boolean result = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1,RedisConst.SKULOCK_EXPIRE_PX2,TimeUnit.SECONDS);
                if (result){
                    try {
                        //result= true；获取到锁，执行业务
                        skuInfo = getSkuInfoDB(skuId);
                        //数据库不存在这个数据
                        if (skuInfo == null){
                            // 防止缓存穿透设置null
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey , skuInfo1 , RedisConst.SKUKEY_TEMPORARY_TIMEOUT , TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //直接将数据放入缓存
                        redisTemplate.opsForValue().set(skuKey , skuInfo , RedisConst.SKUKEY_TIMEOUT , TimeUnit.SECONDS);
                        return skuInfo;
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                }else {
                    // 如果没有获取到锁，睡眠，自旋
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else {
                // 缓存有数据，直接还回
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 数据库兜底操作
        return getSkuInfoDB(skuId);
//        return getSkuInfoByRedisLock(skuId);
    }

    private SkuInfo getSkuInfoByRedissonLock(Long skuId) {
        //  声明一个对象
        SkuInfo skuInfo=null;
        try {
            //  先组成缓存的key  ： 见名之意 ,key 不能重复
            //  key = sku:skuId:info    value = SkuInfo
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //  获取缓存中的数据  存储数据类型是字符串！
            skuInfo = (SkuInfo) this.redisTemplate.opsForValue().get(skuKey);   // 不会写入数据的时候：规定的明确的数据类型！ 通过程序写入
            //  String num = (String) redisTemplate.opsForValue().get("num");  // Integer  String   set num 0 手动填写的
            //  判断缓存中是否有数据
            if (skuInfo==null){
                //  缓存中没有数据,查询数据库 ,要注意 防止缓存击穿，使用分布式锁限制！
                //  redis 原生命令  set key value ex timeout nx;
                //  定义一个锁的key  key = sku:skuId:lock
                String locKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //  准备一个uuid 口令串
                String uuid = UUID.randomUUID().toString();
                //  上锁  1s 锁就过期了.
                Boolean result = this.redisTemplate.opsForValue().setIfAbsent(locKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //  判断
                if (result){
                    //  获取到了锁，查询数据库
                    skuInfo = this.getSkuInfoDB(skuId);
                    //  继续判断从数据库查询到的对象是否为空
                    if(skuInfo==null){
                        //  防止缓存穿透，暂时给一个空对对象进去  10 分钟之后key 过期！
                        SkuInfo skuInfo1 = new SkuInfo();
                        this.redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        //  返回
                        return skuInfo1;
                    }
                    //  将真实数据存储到缓存
                    this.redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //  执行业务结束了,删除锁！  lua 脚本
                    //  定义lua脚本
                    String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                            "then\n" +
                            "    return redis.call(\"del\",KEYS[1])\n" +
                            "else\n" +
                            "    return 0\n" +
                            "end";
                    //  创建一个对象
                    DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
                    defaultRedisScript.setScriptText(scriptText);
                    defaultRedisScript.setResultType(Long.class);
                    //  调用lua脚本
                    //  第一个参数 RedisScript 接收lua脚本的对象 第二个参数 key  第三个参数 value
                    this.redisTemplate.execute(defaultRedisScript, Arrays.asList(locKey),uuid);
                    return skuInfo;
                } else {
                    //    睡眠等待自旋
                    try {
                        Thread.sleep(1000);
                        return getSkuInfo(skuId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                //  缓存有数据
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  查询数据 数据库兜底操作.
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        // 查询skuInfo + skuImageList
        // skuId = sku_info.id 相当于主键
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (skuInfo != null){
            //还需查询图片列表
            //select * from sku_image where sku_id = ? and is_delete = 0;
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id" , skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            //赋值商品的图片列表
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    /**
     * 根据三级分类id 获取分类信息
     */
    @GmallCache(prefix = "CategoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        //select * from v_cate where id = 61;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    /**
     * 根据spuId,skuId 获取销售属性+销售属性值+锁定
     */
    @GmallCache(prefix = "SpuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId , spuId);
    }

    @Override
    /**
     * 根据spuId 获取海报数据
     */
    @GmallCache(prefix = "SpuPosterBySpuId")
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        QueryWrapper<SpuPoster> spuPosterQueryWrapper = new QueryWrapper<>();
        spuPosterQueryWrapper.eq("spu_id" , spuId);
        List<SpuPoster> spuPosterList = spuPosterMapper.selectList(spuPosterQueryWrapper);
        return spuPosterList;
    }

    @Override
    /**
     * 根据skuId 获取平台属性数据
     */
    @GmallCache(prefix = "AttrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(skuId);
    }


    @Override
    /**
     * 根据spuId 获取到销售属性值Id 与skuId 组成的数据集
     */
    @GmallCache(prefix = "SkuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {
        HashMap<Object, Object> hashMap = new HashMap<>();
        List<Map> mapList = skuAttrValueMapper.selectSkuValueIdsMap(spuId);
        if (!CollectionUtils.isEmpty(mapList)){
            mapList.forEach(map -> {
                hashMap.put(map.get("value_ids") , map.get("sku_id"));
            });
        }
        return hashMap;
    }

    @Override
    /**
     * 查询分类数据
     */
    @GmallCache(prefix = "BaseCategoryList:")
    public List<JSONObject> getBaseCategoryList() {
        // 创建一个集合对象
        List<JSONObject> list = new ArrayList<>();
        // 存储分类数据，调用JSONObject类中的put方法
        // 先查询到所有的分类数据集合
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 获取一级分类名称，只需要一个去重操作
        // 按照一级分类id 进行分组 key = category1Id   value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> category1Map =
                baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = category1Map.entrySet().iterator();

        //声明一个index遍历
        int index = 1;
        while (iterator.hasNext()){
            // 获取数据
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            //获取去到一级分类Id
            Long category1Id = entry.getKey();
            //获取一级分类的名称
            List<BaseCategoryView> baseCategoryViewList1 = entry.getValue();
            String category1Name = baseCategoryViewList1.get(0).getCategory1Name();
            // 创建一个属于一级分类的对象
            JSONObject category1 = new JSONObject();
            category1.put("index" , index);
            category1.put("categoryId" , category1Id);
            category1.put("categoryName" , category1Name);

            //categoryChild 要赋值一级分类下的二级数据集合
            //category1.put("categoryChild" , "");  //但是目前我们这里没有二级分类集合。所以先空着.
            index++;

            //创建一个二级分类数据集合
            ArrayList<JSONObject> categoryChild2 = new ArrayList<>();
            //获取二级分类数据，以及二级分类id做分组
            Map<Long, List<BaseCategoryView>> category2Map =
                    baseCategoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = category2Map.entrySet().iterator();
            while (iterator1.hasNext()){
                Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
                // 获取key
                Long category2Id = entry1.getKey();
                // 获取value
                List<BaseCategoryView> baseCategoryViewList2 = entry1.getValue();
                String category2Name = baseCategoryViewList2.get(0).getCategory2Name();
                //创建分类对象，将数据赋值给它
                JSONObject category2 = new JSONObject();
                category2.put("categoryId" , category2Id);
                category2.put("categoryName" , category2Name);

                // 将二级分类对象添加到集合中
                categoryChild2.add(category2);

                // 声明一个三级分类对象集合
                ArrayList<JSONObject> categoryChild3 = new ArrayList<>();
                //获取三级分类数据。不需要在分组，三级分类id都不一样
                baseCategoryViewList2.stream().forEach(baseCategoryView -> {
                    Long category3Id = baseCategoryView.getCategory3Id();
                    String category3Name = baseCategoryView.getCategory3Name();
                    //创建二级分类对象，将数据赋值给他
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId" , category3Id);
                    category3.put("categoryName" , category3Name);
                    //将三级分类对象添加到集合中
                    categoryChild3.add(category3);
                });
                //将三级分类数据添加到二级对象中
                category2.put("categoryChild" , categoryChild3);
            }
            //将二级分类数据添加到集合中
            category1.put("categoryChild" , categoryChild2);
            // 需要将所有的一级分类对象添加到list中
            list.add(category1);
        }
        // 返回数据
        return list;
    }
}
































//
//package com.atguigu.gmall.product.service.impl;
//
//        import com.atguigu.gmall.common.result.Result;
//        import com.atguigu.gmall.model.product.*;
//        import com.atguigu.gmall.product.mapper.*;
//        import com.atguigu.gmall.product.service.ManageService;
//        import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//        import com.baomidou.mybatisplus.core.metadata.IPage;
//        import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//        import io.swagger.annotations.ApiOperation;
//        import org.springframework.beans.factory.annotation.Autowired;
//        import org.springframework.stereotype.Service;
//        import org.springframework.transaction.annotation.Transactional;
//        import org.springframework.util.CollectionUtils;
//        import org.springframework.web.bind.annotation.PostMapping;
//        import org.springframework.web.bind.annotation.RequestBody;
//
//        import java.util.List;
//
//@Service
//public class ManageServiceImpl implements ManageService {
//
//    @Autowired
//    private BaseCategory1Mapper baseCategory1Mapper;
//
//    @Autowired
//    private BaseCategory2Mapper baseCategory2Mapper;
//
//    @Autowired
//    private BaseCategory3Mapper baseCategory3Mapper;
//
//    @Autowired
//    private BaseAttrInfoMapper baseAttrInfoMapper;
//
//    @Autowired
//    private BaseAttrValueMapper baseAttrValueMapper;
//
//    @Autowired
//    private SpuInfoMapper spuInfoMapper;
//
//    @Autowired
//    private BaseTrademarkMapper baseTrademarkMapper;
//
//    @Autowired
//    private BaseSaleAttrMapper baseSaleAttrMapper;
//
//    @Autowired
//    private SpuImageMapper spuImageMapper;
//
//    @Autowired
//    private SpuPosterMapper spuPosterMapper;
//
//    @Autowired
//    private SpuSaleAttrMapper spuSaleAttrMapper;
//
//    @Autowired
//    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
//
//    @Autowired
//    private SkuAttrValueMapper skuAttrValueMapper;
//
//    @Autowired
//    private SkuImageMapper skuImageMapper;
//
//    @Autowired
//    private SkuInfoMapper skuInfoMapper;
//
//    @Autowired
//    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
//    @Override
//    /**
//     * 查询所有一级分类数据
//     */
//    public List<BaseCategory1> getCategory1() {
//        //select * from base_category1
//        return baseCategory1Mapper.selectList(null);
//    }
//
//    @Override
//    /**
//     * 根据一级分类id获取二级分类数据
//     */
//    public List<BaseCategory2> getCategory2(Long category1Id) {
//        //select * from base_category2 where category1_id = ?
//        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id" , category1Id));
//    }
//
//    @Override
//    /**
//     * 根据二级分类id获取三级分类数据
//     */
//    public List<BaseCategory3> getCategory3(Long category2Id) {
//        //select * from base_category3 where category2_id = ?
//        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id" , category2Id));
//    }
//
//    /**
//     * 根据分类Id来查询平台属性！
//     */
//    @Override
//    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
//        // 本质：执行多表关联查询！ 使用xml 文件来解决、mybatis
//        return baseAttrInfoMapper.selectAttrInfoList(category1Id , category2Id ,category3Id);
//    }
//
//    /**
//     *保存平台属性
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)//如果有异常则回滚,如果不写 rollbackFor = Exception.class ，只有发生运行时异常才会回滚！
//    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
//        //  base_attr_info   base_attr_value
//
//        //修改的时候，baseAttrInfo.id 一定存在的！
//        if (baseAttrInfo.getId() == null){
//            //  执行完成插入数据之后， @TableId(type = IdType.AUTO) 表示插入完成之后，就可以获取到当前的主键Id
//            baseAttrInfoMapper.insert(baseAttrInfo);
//        }else {
//            //修改数据：
//            baseAttrInfoMapper.updateById(baseAttrInfo);
//            //  平台属性值修改： base_attr_value 无法确定用户到底要做 UPDATE ,DELETE ,INSERT 因此 ：先del 再 insert
//            //  虽然调用的delete 方法。但是我们做的是逻辑删除！ 本质：是执行的update 操作，非 delete 操作！
//            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
//            baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
//            baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
//        }
//        //获取到平台属性集合数据
//        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
//        //保存
//        if (!CollectionUtils.isEmpty(attrValueList)){
//            //  遍历数据保存到数据库表
//            attrValueList.forEach((baseAttrValue )-> {
//                //  base_attr_value.attr_id 页面提交的时候，不会携带这个字段数据.
//                //  base_attr_value.attr_id = base_attr_info.id
//                baseAttrValue.setAttrId(baseAttrInfo.getId());
//                baseAttrValueMapper.insert(baseAttrValue);
//            });
//        }
//    }
//
//    /**
//     * 回显平台属性数据
//     */
//    @Override
//    public List<BaseAttrValue> getAttrValueList(Long attrId) {
//        return baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id",attrId));
//    }
//    @Override
//    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
//        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
//        //赋值平台属性值集合数据
//        if (baseAttrInfo != null){
//            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
//        }
//        return baseAttrInfo;
//    }
//
//    /**
//     * spu 分页列表
//     */
//    @Override
//    public IPage<SpuInfo> getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
//        // select *from spu_info where category3_id = 61 order by id desc limit 0 , 10;
//        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
//        spuInfoQueryWrapper.eq("category3_id" , spuInfo.getCategory3Id());
//        spuInfoQueryWrapper.orderByDesc("id");//排序
//        return spuInfoMapper.selectPage(spuInfoPage , spuInfoQueryWrapper);
//    }
//
//    /**
//     * 品牌分业列表
//     */
//    @Override
//    public IPage<BaseTrademark> getTradeMarkList(Page<BaseTrademark> baseTrademarkPage) {
//        // select *from base_trademark order by id desc limit 0 , 10;
//        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
//        baseTrademarkQueryWrapper.orderByDesc("id");
//        return baseTrademarkMapper.selectPage(baseTrademarkPage , baseTrademarkQueryWrapper);
//    }
//
//
//    /**
//     * 获取销售属性列表
//     */
//    @Override
//    public List<BaseSaleAttr> getBaseSaleAttrList() {
//        //select * from base_sale_attr where is_deleted = 0;
//
//        return baseSaleAttrMapper.selectList(null);
//    }
//
//    /**
//     *保存spu
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void saveSpuInfo(SpuInfo spuInfo) {
//        /*
//            spu_image
//            spu_info
//            spu_poster
//            spu_sale_attr
//            spu_sale_attr_value
//         */
//        spuInfoMapper.insert(spuInfo);
//        // 获取到spuImage List 集合
//        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
//        // 判断当前集合不为空，循环遍历插入
//        if (!CollectionUtils.isEmpty(spuImageList)){
//            spuImageList.forEach(spuImage -> {
//                // 赋值spuId
//                spuImage.setSpuId(spuInfo.getId());
//                spuImageMapper.insert(spuImage);
//            });
//        }
//        // 获取商品海报数据集合
//        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
//        // 判断当前集合不为空，循环遍历插入
//        if (!CollectionUtils.isEmpty(spuPosterList)){
//            spuPosterList.forEach(spuPoster -> {
//                // 赋值spuId
//                spuPoster.setSpuId(spuInfo.getId());
//                spuPosterMapper.insert(spuPoster);
//            });
//        }
//        //获取商品的销售属性
//        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
//        // 判断当前集合不为空，循环遍历插入
//        if (!CollectionUtils.isEmpty(spuSaleAttrList)){
//            spuSaleAttrList.forEach(spuSaleAttr -> {
//                //赋值spuId
//                spuSaleAttr.setSpuId(spuInfo.getId());
//                spuSaleAttrMapper.insert(spuSaleAttr);
//
//                // 获取到销售属性值的集合数据
//                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
//                // 判断当前集合不为空，循环遍历插入
//                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
//                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
//                        //赋值spuId
//                        spuSaleAttrValue.setSpuId(spuSaleAttr.getId());
//                        //赋值销售属性名
//                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
//
//                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
//                    });
//                }
//            });
//        }
//    }
//
//    /**
//     * 根据spuId获取所有的图片列表
//     */
//    @Override
//    public List<SpuImage> getSpuImageList(Long spuId) {
//        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id" , spuId));
//    }
//
//    /**
//     * 根据spuId 查询销售属性
//     */
//    @Override
//    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
//        //调用mapper层
//        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
//    }
//
//    /**
//     * 保存SkuInfo
//     */
//    @Override
//    public void getSaveSkuInfo(SkuInfo skuInfo) {
//        /*
//            sku_info
//            sku_image
//            sku_attr_value
//            sku_sale_attr_value
//         */
//        skuInfoMapper.insert(skuInfo);
//
//        //  获取skuImage list 集合数据
//        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
//        if (!CollectionUtils.isEmpty(skuImageList)){
//            //  循环遍历
//            skuImageList.forEach(skuImage -> {
//                //  赋值skuId
//                skuImage.setSkuId(skuInfo.getId());
//                skuImageMapper.insert(skuImage);
//            });
//        }
//
//        //  sku_attr_value 获取到平台属性集合数据
//        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
//        if (!CollectionUtils.isEmpty(skuAttrValueList)){
//            //  循环遍历
//            skuAttrValueList.forEach(skuAttrValue -> {
//                //  赋值skuId
//                skuAttrValue.setSkuId(skuInfo.getId());
//                skuAttrValueMapper.insert(skuAttrValue);
//            });
//        }
//        //  sku_sale_attr_value 获取到销售属性数据
//        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
//        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
//            //  循环遍历
//            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
//                //  赋值操作：
//                skuSaleAttrValue.setSkuId(skuInfo.getId());
//                skuSaleAttrValue.setSpuId(skuInfo.getSpuId()); // 在保存数据的时候，将spuId 赋值到skuInfo
//                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
//            });
//        }
//
//
//    }
//
//    @Override
//    public void onSale(Long skuId) {
//        //  更新 update sku_info set is_sale = 1 where id = skuId;
//        SkuInfo skuInfo = new SkuInfo();
//        skuInfo.setId(skuId);
//        skuInfo.setIsSale(1);
//        this.skuInfoMapper.updateById(skuInfo);
//    }
//
//    @Override
//    public void cancelSale(Long skuId) {
//        //  更新 update sku_info set is_sale = 0 where id = skuId;
//        SkuInfo skuInfo = new SkuInfo();
//        skuInfo.setId(skuId);
//        skuInfo.setIsSale(0);
//        this.skuInfoMapper.updateById(skuInfo);
//    }
//
//    @Override
//    public IPage getSkuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo) {
//        //  select * from sku_info where category3_id = 61 order by id desc limit 0,10;
//        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
//        skuInfoQueryWrapper.eq("category3_id",skuInfo.getCategory3Id());
//        skuInfoQueryWrapper.orderByDesc("id");
//        return skuInfoMapper.selectPage(skuInfoPage,skuInfoQueryWrapper);
//    }
//}















