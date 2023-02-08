package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {
    /**
     * 根据三级分类 id 获取品牌列表
     */
    List<BaseTrademark> getTrademarkList(Long category3Id);

    /**
     * 根据三级分类 id 获取可选择分类列表
     */
    List<BaseTrademark> getCurrentTrademarkList(Long category3Id);

    /**
     * 保存分类品牌关联
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 删除分类品牌关联
     */
    void removeByCategory3IdAndTmId(Long category3Id, Long trademarkId);
}
