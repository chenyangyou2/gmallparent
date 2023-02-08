package com.atguigu.gmall.model.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.list.repository.GoodsRepository;
import com.atguigu.gmall.model.list.service.SearchService;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    // 编写上架功能
    public void upperGoods(Long skuId) {
        // 创建一个goods 对象
        Goods goods = new Goods();
        // 获取skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        goods.setId(skuId);
        // 商品的名称
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        // 查询实时价格
        goods.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());
        goods.setCreateTime(new Date());

        //设置品牌
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        goods.setTmId(trademark.getId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        // 设置分类数据
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());
        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());

        // 设置商品的平台属性 将当前这个skuId 对应平台属性保持到es
        // skuId = 24 那么它对应的平台属性 属性值有哪些？ 会将其他都保存到es 中
        // 手机系统 手机品牌 机身内存 运行内存
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        // 赋值平台属性集合
        goods.setAttrs(searchAttrList);
        // 保存方法
        goodsRepository.save(goods);


    }

    @Override
    // 编写下架功能
    public void lowerGoods(Long skuId) {
        //删除数据
        goodsRepository.deleteById(skuId);
    }

    @Override
    /**
     * 商品热度排名
     */
    public void incrHotScore(Long skuId) {
        // 需要借用redis 来缓冲
        String hotkey = "hotScore";
        //  String[incr number]  zSet [zincrby hotScore 1 sku:23:info]
        Double count = redisTemplate.opsForZSet().incrementScore(hotkey, "hot" + skuId, 1);

        // 判断
        if (count % 10 == 0){
            // 此时更新一次es中的hotScore
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(count.longValue());
            goodsRepository.save(goods);
        }
    }
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        /*
        1.动态生成dsl 语句，restHighLevelClient
        2.根据这个dsl 语句执行，获取结果
        3.将结果 封装到 SearchResponseVo
         */
        SearchRequest searchRequest = searchDsl(searchParam);
        // 调用查询方法
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        // 将结果 封装到SearchResponseVo
        SearchResponseVo searchResponseVo = parseResult(searchResponse);

        /*
         private List<SearchResponseTmVo> trademarkList;
         private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
         private List<Goods> goodsList = new ArrayList<>();
         private Long total;//总记录数

         private Integer pageSize;//每页显示的内容
         private Integer pageNo;//当前页面
         private Long totalPages;

         */

        searchResponseVo.setPageSize(searchParam.getPageSize());
        searchResponseVo.setPageNo(searchParam.getPageNo());

        long totalPages = (searchResponseVo.getTotal()+searchParam.getPageSize()-1/searchParam.getPageSize());
        searchResponseVo.setTotalPages(totalPages);
        return searchResponseVo;
    }

    /**
     * 数据结果集转换
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        /*
            在这个方法中给以下四个属性赋值
            private List<SearchResponseTmVo> trademarkList;
            private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            private List<Goods> goodsList = new ArrayList<>();
            private Long total;//总记录数
         */
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        //hits total value
        searchResponseVo.setTotal(hits.getTotalHits().value);
        // 赋值商品列表 从——source获取
        // 声明一个集合来储存商品
        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] subHits = hits.getHits();
        for (SearchHit subHit : subHits) {
            //  _source 对应的字符串
            String sourceAsString = subHit.getSourceAsString();
            // 将字符串转为Goods
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            /*
                稍微有点问题：在查询数据的时候，如果是根据关键词检索，则应该高亮！
                如果是高亮，商品名称就不应该获取Goods 中的title，而且获取高亮字段
             */
            if (subHit.getHighlightFields().get("title") != null){
                //  表示有高亮数据  为什么0 ? 因为title 对应的这个数组中只有一条数据！
                Text[] title= subHit.getHighlightFields().get("title").getFragments();
                // 设置高亮的商品名称
                goods.setTitle(title.toString());
            }
            goodsList.add(goods);
        }
        searchResponseVo.setGoodsList(goodsList);

        // 获取品牌信息：
//        List<SearchResponseTmVo> trademarkList = new ArrayList<>();

        // key = tmIdAgg value = Aggregation
        Map<String, Aggregation> stringAggregationMap = searchResponse.getAggregations().asMap();
        //  Aggregation 接口：转换为 ParsedLongTerms 实现类 目的为了获取到getBuckets();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) stringAggregationMap.get("tmIdAgg");
        // buckets
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            // 声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 获取到品牌的Id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            // 获取到品牌的名称
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            // 获取品牌id 对应的品牌名只有一个
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 获取到品牌的LogoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            //  因为品牌Id 对应的品牌LogoUrl 只有一个
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            return searchResponseTmVo;
        }).collect(Collectors.toList());

        searchResponseVo.setTrademarkList(trademarkList);


        // 获取平台属性数据：平台属性数据类型nested
        ParsedNested attrAgg = (ParsedNested) stringAggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");

        // 获取到平台属性值集合数据
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            //创建一个平台属性对象
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            // 获取到平台属性Id
            String attrId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrId));

            //  获取到平台属性名
            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            //  因为平台属性Id 对应的平台属性名 只有一个
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            //  获取到平台属性值名
            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<String> attrValueList =
                    attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(attrValueList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        searchResponseVo.setAttrsList(attrsList);

        return searchResponseVo;
    }

    /**
     * 动态生成dsl 语句
     * @param searchParam
     * @return
     */
    private SearchRequest searchDsl(SearchParam searchParam) {
        //  构建一个查询器：{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //  {query bool }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //  判断是否 根据关键词检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //  {bool must match }
            boolQuery.must(QueryBuilders.matchQuery("title",searchParam.getKeyword()).operator(Operator.AND));

            //  设置高亮 { highlight }
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }
        //  根据分类Id 进行检索：
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //  {query bool filter term}
            boolQuery.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //  {query bool filter term}
            boolQuery.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //  {query bool filter term}
            boolQuery.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }

        //  按照品牌进行过滤 trademark=3:华为
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            //  需要进行一次分割
            String[] split = searchParam.getTrademark().split(":");
            if (split!=null && split.length == 2){
                //  {query bool filter term }
                boolQuery.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }

        //  根据平台属性值Id 进行过滤
        //  props=23:8G:运行内存&props=24:256G:机身内存
        String[] props = searchParam.getProps();
        if (props!=null && props.length >0){
            //  循环遍历
            for (String prop : props) {
                // prop  props=23:8G:运行内存
                String[] split = prop.split(":");
                if (split!=null && split.length ==3){
                    //  构建 dsl 语句. 外层
                    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolBuilder = QueryBuilders.boolQuery();

                    subBoolBuilder.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolBuilder.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    //  subBoolBuilder ---> boolQueryBuilder
                    boolQueryBuilder.must(QueryBuilders.nestedQuery("attrs",subBoolBuilder, ScoreMode.None));
                    boolQuery.filter(boolQueryBuilder);
                }
            }
        }


        //  { query }
        searchSourceBuilder.query(boolQuery);

        //  排序：价格降序 order=2:desc 价格升序 order=2:asc  综合降序 order=1:desc  综合升序 order=1:asc
        //  先获取到用户传递的数据
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            //  分割字符串
            String[] split = order.split(":");
            if (split!=null && split.length ==2){
                //  声明一个字符串 记录 按照什么方式排序
                String field = "";
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                }
                //  第一个参数是排序的字段，第二个参数是排序的规则
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            } else {
                //  默认排序规则
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }
        //  分页： 5条数据 第一页 0,3 第二页 3,3
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //  设置每页显示的大小:默认3条
        searchSourceBuilder.size(searchParam.getPageSize());

        //  聚合： 品牌 agg - term -- field
        searchSourceBuilder.aggregation( AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"))
        );
        //  聚合： 平台属性 ---> 数据类型是nested
        searchSourceBuilder.aggregation( AggregationBuilders.nested("attrAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                )
        );

        //  声明一个对象  GET /goods/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        //  在查询的时候， id，defaultImg,title,price,createTime 这些字段是我们需要的: Goods
        //  品牌,平台属性相关数据都是从 聚合中获取的！不从Goods 实体类中获取。
        searchSourceBuilder.fetchSource(new String []{"id","defaultImg","title","price","createTime"},null);
        //  把整个的query ，sort , from。。。 所有的dsl 都放入 source 中
        searchRequest.source(searchSourceBuilder);

        //  dsl 语句都是封装到 searchSourceBuilder 类中
        System.out.println("dsl:\t"+searchSourceBuilder.toString());
        //  返回对象
        return searchRequest;
    }


}


































































