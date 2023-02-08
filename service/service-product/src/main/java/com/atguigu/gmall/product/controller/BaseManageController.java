package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController//@ResponseBody + @Controller
@RequestMapping("/admin/product")//映射URL路径
//@CrossOrigin
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    /**
     *获取所有一级分类数据
     * 后台返回值类型 Result
     * /admin/product/getCategory1
     */
    @ApiOperation("获取一级分类数据")
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //控制器调用服务层方法
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        //需要将后台查询数据的数据返回给页面渲染
        return Result.ok(baseCategory1List);
        //Result<T> result = build(data); data = baseCategory1List
        //Result<T> result = new Result();  result.setData(baseCategory1List);  将集合赋值给Result 对象中的data 属性！
    }

    /**
     * /admin/product/getCategory2/{category1Id}
     *  根据一级分类id获取二级分类数据
     */
    @ApiOperation("获取二级分类数据")
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        //调用服务层方法
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    /**
     *  /admin/product/getCategory3/{category2Id}
     *   根据二级分类id获取三级分类数据
     */
    @ApiOperation("获取三级分类数据")
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        //调用服务层方法
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    /**
     *  /admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
     * 根据分类Id来查询平台属性！
     */
    @ApiOperation("查询平台属性")
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id ,
                                  @PathVariable Long category2Id ,
                                  @PathVariable Long category3Id ){
        List<BaseAttrInfo> baseAttrInfoList =
                manageService.getAttrInfoList( category1Id , category2Id , category3Id);
        return Result.ok(baseAttrInfoList);
    }

    /**
     * 回显平台属性数据
     * /admin/product/getAttrValueList/{attrId}
     */
    @ApiOperation("回显平台属性数据")
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
        //  从功能上讲： 这个是没有问题的！
        //  调用服务层方法  select * from base_attr_value where attr_id = ? and is_deleted = 0;
        //  List<BaseAttrValue> baseAttrValueList = this.manageService.getAttrValueList(attrId);
        //  回显平台属性值集合
        //  return Result.ok(baseAttrValueList);
        BaseAttrInfo baseAttrInfo = manageService.getBaseAttrInfo(attrId);
        return Result.ok(baseAttrInfo.getAttrValueList());
    }




    /**
     * /admin/product/saveAttrInfo
     * 保存平台属性
     * 页面提交过来的数据格式：Json ---->  JavaObject！  @RequestBody 将json 变为java对象
     */
    @ApiOperation("保存平台属性")
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }
}
