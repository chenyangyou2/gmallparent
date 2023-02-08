package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {

    /**
     * 根据分类Id来查询平台属性！
     */
    List<BaseAttrInfo> selectAttrInfoList(@Param("category1Id") Long category1Id , @Param("category2Id") Long category2Id , @Param("category3Id") Long category3Id);

    /**
     * 根据skuId 获取平台属性数据
     */
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("skuId") Long skuId);
}
