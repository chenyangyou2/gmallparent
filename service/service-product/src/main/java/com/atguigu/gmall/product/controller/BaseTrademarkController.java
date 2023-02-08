package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark/")
public class BaseTrademarkController {

//    @Autowired
//    private ManageService manageService;

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 品牌查询
     * /admin/product/baseTrademark/{page}/{limit}
     * 品牌分业列表
     */
    @ApiOperation("品牌分业列表")
    @GetMapping("{page}/{limit}")
    public Result getTradeMarkList(@PathVariable Long page ,
                                   @PathVariable Long limit){
        // 构建Page对象
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page, limit);
//        IPage<BaseTrademark> baseTrademarkIPage = manageService.getTradeMarkList(baseTrademarkPage);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.getTradeMarkList(baseTrademarkPage);
        // 返回数据
        return Result.ok(baseTrademarkIPage);
    }

    /**
     * 品牌保存
     * /admin/product/baseTrademark/save
     * vue 项目保存是传递的数据都是Json Json ----> javaObject
     */
    @ApiOperation("品牌保存")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        /*
            调用服务层方法
            自定义 this.manageService.save();
            第二个可以使用IService 接口
            this.getBaseMapper().insert(entity));  ===  baseTrademarkMapper.insert(baseTrademark);
         */
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    /**
     * 数据回显
     * /admin/product/baseTrademark/get/{id}
     */
    @ApiOperation("数据回显")
    @GetMapping("get/{id}")
    public Result getById(@PathVariable Long id){
        BaseTrademark byId = baseTrademarkService.getById(id);
        return Result.ok(byId);
    }

    /**
     * 修改商品数据
     * /admin/product/baseTrademark/update
     */
    @ApiOperation("修改商品数据")
    @PutMapping("update")
    public Result updateTradeMark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    /**
     * 删除商品
     * /admin/product/baseTrademark/remove/{id}
     */
    @ApiOperation("删除商品")
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }
}


























