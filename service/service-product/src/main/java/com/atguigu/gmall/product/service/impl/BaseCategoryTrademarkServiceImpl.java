package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper , BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    /**
     * 根据三级分类 id 获取品牌列表
     */
    public List<BaseTrademark> getTrademarkList(Long category3Id) {
        // 根据category3Id 获取已绑定的品牌
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id" , category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        //  返回集合列表 ；只需要trademark_id 字段！ 将其组成 集合！
        //  使用拉姆达表达式做映射获取到 品牌Id 集合数据.
        //  第二种使用拉姆达表达式！
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //拉姆达表达式的参数如何辨别：集合泛型是谁！  有参有返回！
            List<Long> tmIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //再根据这个品牌的Id 集合 查询 base_trademark
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectBatchIds(tmIdList);
            return baseTrademarkList;
        }
        return null;


//        // 判断分类Id 下有的品牌
//        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
//            //  循环遍历获取到当前已绑定的品牌Id 集合！ # 1,2,3
//            List<Long> tmIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
//                return baseCategoryTrademark.getTrademarkId();
//            }).collect(Collectors.toList());
//
//            //查询所有品牌id集合 ， 然后将已经绑定的品牌id 集合过滤掉   有参有返回值
//            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
//                //总的品牌id baseTrademark.getId();
//                return !tmIdList.contains(baseTrademark.getId());
//            }).collect(Collectors.toList());
//            return baseTrademarkList;
//        }
//        // 分类Id 下没有品牌列表 查询所有
//        return baseTrademarkMapper.selectList(null);
    }

    @Override
    public List<BaseTrademark> getCurrentTrademarkList(Long category3Id) {
        // 根据category3Id 获取已绑定的品牌
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        // 判断分类Id 下有的品牌
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            //  循环遍历获取到当前已绑定的品牌Id 集合！ # 1,2,3
            List<Long> tmIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());

            //查询所有品牌id集合 ， 然后将已经绑定的品牌id 集合过滤掉   有参有返回值
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                //总的品牌id baseTrademark.getId();
                return !tmIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            return baseTrademarkList;
        }
        // 分类Id 下没有品牌列表 查询所有
        return baseTrademarkMapper.selectList(null);
    }

    @Override
    /**
     * 保存分类品牌关联
     */
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        //  本质：base_category_trademark 插入数据.
        //  获取品牌Id 集合
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        //批量注入
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(tmId ->{
                //  声明一个对象
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                baseCategoryTrademark.setTrademarkId(tmId);
                return baseCategoryTrademark;
            }).collect(Collectors.toList());
            //调用批量保存方法
            saveBatch(baseCategoryTrademarkList);

        }
    }

    @Override
    /**
     * 删除分类品牌关联
     */
    public void removeByCategory3IdAndTmId(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id" , category3Id);
        baseCategoryTrademarkQueryWrapper.eq("trademark_id" , trademarkId);
        baseCategoryTrademarkMapper.delete(baseCategoryTrademarkQueryWrapper);
    }
}























