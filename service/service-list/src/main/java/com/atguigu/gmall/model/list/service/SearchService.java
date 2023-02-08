package com.atguigu.gmall.model.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

public interface SearchService {
    // 编写上架功能
    void upperGoods(Long skuId);

    // 编写下架功能
    void lowerGoods(Long skuId);

    //商品热度排名
    void incrHotScore(Long skuId);

    //检索
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
