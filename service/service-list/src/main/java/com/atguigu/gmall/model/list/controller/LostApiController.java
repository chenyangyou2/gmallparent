package com.atguigu.gmall.model.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import com.atguigu.gmall.model.list.service.SearchService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class LostApiController {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private SearchService searchService;

    // 编写一个控制器
    @GetMapping("inner/createIndex")
    public Result createIndex(){
        // 调用操作es api的对象
        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    // 编写上架功能
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    // 编写下架功能
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /**
     * 商品热度排名
     * /api/list/inner/incrHotScore/{skuId}
     */
    @ApiOperation("商品热度排名")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    /**
     * 全文检索
     * /api/list
     */
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo = null;
        try {
            searchResponseVo = searchService.search(searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.ok(searchResponseVo);
    }
}

























