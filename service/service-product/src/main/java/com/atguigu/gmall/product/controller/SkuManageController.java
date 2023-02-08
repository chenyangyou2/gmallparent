package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 保存SkuInfo
     * /admin/product/saveSkuInfo
     */
    @ApiOperation("保存SkuInfo")
    @PostMapping("saveSkuInfo")
    public Result getSaveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.getSaveSkuInfo(skuInfo);
        return Result.ok();
    }

    /**
     * sku商品分页
     * /admin/product/list/{page}/{limit}
     */
    @ApiOperation("sku商品分页")
    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page ,
                                 @PathVariable Long limit ,
                                 SkuInfo skuInfo){
        Page<SkuInfo> skuInfoPage = new Page<>(page , limit);
        IPage iPage = manageService.getSkuInfoList(skuInfoPage , skuInfo);
        return Result.ok(iPage);
    }

    /**
     * 上架
     * /admin/product/onSale/{skuId}
     */
    @ApiOperation("上架")
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }

    /**
     * 下架
     * /admin/product/cancelSale/{skuId}
     */
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //  调用服务层方法
        this.manageService.cancelSale(skuId);
        return Result.ok();
    }
}























