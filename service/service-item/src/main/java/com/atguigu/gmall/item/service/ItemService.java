package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {

    /**
     * 根据skuId 来获取 商品详情需要的数据
     */
    Map getItem(Long skuId);
}
