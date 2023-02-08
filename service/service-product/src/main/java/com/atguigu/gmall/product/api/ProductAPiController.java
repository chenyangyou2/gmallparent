package com.atguigu.gmall.product.api;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 主要目的是给service-item 提供数据的。所以我们叫他内部数据接口
 */

@RestController
@RequestMapping("/api/product/inner/")
public class ProductAPiController {

    @Autowired
    private ManageService manageService;

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 根据skuId获取SkuInfo
     * /api/product/inner/getSkuInfo/{skuId}
     */
    @GetMapping("getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){

        return this.manageService.getSkuInfo(skuId);
    }

    /**
     * 根据skuId 获取最新的商品价格
     * /api/product/inner/getSkuPrice/{skuId}
     */
    @GetMapping("getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPrice(skuId);
    }

    /**
     * 根据三级分类id 获取分类信息
     * /api/product/inner/getCategoryView/{category3Id}
     */
    @GetMapping("getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getCategoryView(category3Id);
    }

    /**
     * 根据spuId,skuId 获取销售属性+销售属性值+锁定
     * /api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}
     */
    @GetMapping("getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId ,
                                                          @PathVariable Long spuId){
        return manageService.getSpuSaleAttrListCheckBySku(skuId , spuId);
    }

    /**
     * 根据spuId 获取海报数据
     * /api/product/inner/findSpuPosterBySpuId/{spuId}
     */
    @GetMapping("findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        return manageService.findSpuPosterBySpuId(spuId);
    }

    /**
     * 根据skuId 获取平台属性数据
     * /api/product/inner/getAttrList/{skuId}
     */
    @GetMapping("getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return manageService.getAttrList(skuId);
    }

    /**
     * 根据spuId 获取到销售属性值Id 与skuId 组成的数据集
     * /api/product/inner/getSkuValueIdsMap/{spuId}
     */
    @GetMapping("getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return this.manageService.getSkuValueIdsMap(spuId);
    }

    /**
     * 根据品牌Id 获取到品牌对象数据
     * /api/product/inner/getTrademark/{tmId}
     */
    @GetMapping("getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){

        return baseTrademarkService.getById(tmId);
    }
}




























