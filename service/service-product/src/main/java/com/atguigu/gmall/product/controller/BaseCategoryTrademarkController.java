package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/baseCategoryTrademark/")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    /**
     * 根据三级分类 id 获取品牌列表
     * /admin/product/baseCategoryTrademark/findTrademarkList/{category3Id}
     */
    @ApiOperation("根据三级分类 id 获取品牌列表")
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> baseTrademarkList = baseCategoryTrademarkService.getTrademarkList(category3Id);
        return Result.ok(baseTrademarkList);
    }

    /**
     * 根据三级分类 id 获取可选择分类列表
     * /admin/product/baseCategoryTrademark/findCurrentTrademarkList/{category3Id}
     */
    @ApiOperation("根据三级分类 id 获取可选择分类列表")
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> baseTrademarkList = baseCategoryTrademarkService.getCurrentTrademarkList(category3Id);
        return Result.ok(baseTrademarkList);
    }

    /**
     * 保存分类品牌关联
     * /admin/product/baseCategoryTrademark/save
     */
    @ApiOperation("保存分类品牌关联")
    @PostMapping("save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();
    }

    /**
     * 删除分类品牌关联
     * /admin/product/baseCategoryTrademark/remove/{category3Id}/{trademarkId}
     */
    @ApiOperation("删除分类品牌关联")
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id ,
                       @PathVariable Long trademarkId){
        baseCategoryTrademarkService.removeByCategory3IdAndTmId(category3Id , trademarkId);
        return Result.ok();
    }
}





















