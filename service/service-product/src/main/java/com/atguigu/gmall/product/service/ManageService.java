package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    /**
     * 查询所有一级分类数据
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类id获取二级分类数据
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类id获取三级分类数据
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类Id来查询平台属性！
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     *保存平台属性
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);



    /**
     * 回显平台属性数据
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);
    BaseAttrInfo getBaseAttrInfo(Long attrId);




    /**
     * spu 分页列表
     */
    IPage<SpuInfo> getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    /**
     * 品牌分业列表
     */
    IPage<BaseTrademark> getTradeMarkList(Page<BaseTrademark> baseTrademarkPage);

    /**
     * 获取销售属性列表
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     *保存spu
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId获取所有的图片列表
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId 查询销售属性
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存SkuInfo
     */
    void getSaveSkuInfo(SkuInfo skuInfo);

    /**
     * sku商品分页
     */
    IPage getSkuInfoList(Page<SkuInfo> skuInfoPage , SkuInfo skuInfo);

    /**
     * 上架
     */
    void onSale(Long skuId);

    /**
     * 下架
     */
    void cancelSale(Long skuId);

    /**
     * 根据skuId获取SkuInfo
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据skuId 获取最新的商品价格
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据三级分类id 获取分类信息
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 根据spuId,skuId 获取销售属性+销售属性值+锁定
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId 获取海报数据
     */
    List<SpuPoster> findSpuPosterBySpuId(Long spuId);

    /**
     * 根据skuId 获取平台属性数据
     */
    List<BaseAttrInfo> getAttrList(Long skuId);

    /**
     * 根据spuId 获取到销售属性值Id 与skuId 组成的数据集
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 查询分类数据
     */
    List<JSONObject> getBaseCategoryList();
}
