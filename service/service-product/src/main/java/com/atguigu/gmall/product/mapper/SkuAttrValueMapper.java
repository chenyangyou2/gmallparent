package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValue> {

    /**
     * 根据spuId 获取到销售属性值Id 与skuId 组成的数据集
     */
    List<Map> selectSkuValueIdsMap(Long spuId);
}
