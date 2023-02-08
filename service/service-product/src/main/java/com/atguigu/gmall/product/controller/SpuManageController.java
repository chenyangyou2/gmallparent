package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    /**
     *  /admin/product/{page}/{limit}
     *  spu 分页列表
     */
    @ApiOperation("spu分页列表")
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoList(@PathVariable Long page ,
                                 @PathVariable Long limit ,
                                 SpuInfo spuInfo){
        Page<SpuInfo> spuInfoPage = new Page<>(page, limit);
        IPage<SpuInfo> iPage = manageService.getSpuInfoList(spuInfoPage , spuInfo);
        return Result.ok(iPage);
    }

    /**
     * 获取销售属性列表
     * /admin/product/baseSaleAttrList
     */
    @ApiOperation("获取销售属性列表")
    @GetMapping("baseSaleAttrList")
    public Result getBaseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    /**
     * 保存spu
     * /admin/product/saveSpuInfo
     */
    @ApiOperation("保存spu")
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    /**
     * 根据spuId获取所有的图片列表
     * /admin/product/spuImageList/{spuId}
     */
    @ApiOperation("根据spuId获取所有的图片列表")
    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId){
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    /**
     * 根据spuId 查询销售属性
     * /admin/product/spuSaleAttrList/{spuId}
     */
    @ApiOperation("根据spuId 查询销售属性")
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable Long spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }
}
