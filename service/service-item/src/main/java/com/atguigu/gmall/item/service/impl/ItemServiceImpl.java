package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map getItem(Long skuId) {
        HashMap<String, Object> hashMap = new HashMap<>();

        // 判断布隆过滤器中是否有skuId
//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        if (!bloomFilter.contains(skuId)){
//            // 布隆过滤器中没有
//            return null;
//        }

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 获取skuInfo 数据
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //保存一个数据
            hashMap.put("skuInfo", skuInfo);
            // 返回
            return skuInfo;
        },threadPoolExecutor);

        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 获取分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            // 保存数据
            hashMap.put("categoryView", categoryView);
        },threadPoolExecutor);

        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            // 获取最新商品价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            hashMap.put("price" , skuPrice);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 销售属性回显+锁定
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            hashMap.put("spuSaleAttrList" , spuSaleAttrList);
        }, threadPoolExecutor);

        CompletableFuture<Void> skuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 获取切换功能需要的Json 数据
            Map map = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String skuJson = JSON.toJSONString(map);
            System.out.println("skuJson : \t" + skuJson);
            hashMap.put("valuesSkuJson", skuJson);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuPosterListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 获取海报数据
            List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            hashMap.put("spuPosterList", spuPosterList);
        }, threadPoolExecutor);

        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            // 获取规格参数
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            // 声明一个集合来储存Map
            ArrayList<Map> arrayList = new ArrayList<>();
            // 在这个集合中获取到平台属性 平台属性值
            attrList.forEach(baseAttrInfo -> {
                HashMap<String, Object> maps = new HashMap<>();

                maps.put("attrName", baseAttrInfo.getAttrName());
                maps.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                arrayList.add(maps);
            });
            hashMap.put("skuAttrList", arrayList);
        }, threadPoolExecutor);

        //调用热度排名方法
        CompletableFuture<Void> incrCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);
        //  多任务组合：
        CompletableFuture.allOf(skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                skuPriceCompletableFuture,
                spuSaleAttrListCompletableFuture,
                skuJsonCompletableFuture,
                spuPosterListCompletableFuture,
                attrCompletableFuture,
                incrCompletableFuture).join();
        //  返回map集合数据.

        return hashMap;
    }
}
