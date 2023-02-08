
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Controller
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;
    // 全文检索控制器
    @GetMapping("list.html")
    public String list(SearchParam searchParam , Model model){
        Result<Map> result = listFeignClient.list(searchParam);

        //设置排序
        HashMap<String , Object> orderMap = makeOrderMap(searchParam.getOrder());

        //平台属性面包屑
        List<Map> propsParamList = makeProps(searchParam.getProps());

        // 品牌面包屑
        String trademarkParam = mkeTradeMareParam(searchParam.getTrademark());

        // 储存urlParam --- 记录用户通过那些条件经行检索
        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("urlParam" , urlParam);

        model.addAttribute("orderMap",orderMap);
        //储存平台属性面包屑
        model.addAttribute("propsParamList" , propsParamList);
        //储存品牌面包屑
        model.addAttribute("trademarkParam" , trademarkParam);

        model.addAllAttributes(result.getData());
        model.addAttribute("searchParam" , searchParam);
        return "list/index";
    }

    //设置排序
    private HashMap<String, Object> makeOrderMap(String order) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)){
            String[] split = order.split(":");
            if (split != null && split.length == 2){
                hashMap.put("type" , split[0]);
                hashMap.put("sort" , split[1]);
            }
        }else {
            hashMap.put("type" , "1");
            hashMap.put("sort" , "desc");
        }
        return hashMap;
    }

    //平台属性面包屑
    private List<Map> makeProps(String[] props) {
        // 声明一个集合
        ArrayList<Map> mapList = new ArrayList<>();
        if (props != null && props.length > 0){
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3){
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("attrId" , split[0]);
                    hashMap.put("attrValue" , split[1]);
                    hashMap.put("attrName" , split[2]);
                    mapList.add(hashMap);
                }
            }
        }
        return mapList;
    }

    // 品牌面包屑
    private String mkeTradeMareParam(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            //字符串分割
            String[] split = trademark.split(":");
            if (split != null && split.length == 2){
                return "品牌: " + split[1];
            }
        }
        return null;
    }

    // 记录当前用户通过那些条件经行了检索
    private String makeUrlParam(SearchParam searchParam) {
        // 字符串拼接
        StringBuilder sb = new StringBuilder();

        // 第一个入口：分类
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            sb.append("category3Id=").append(searchParam.getCategory3Id());
        }

        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            sb.append("category2Id=").append(searchParam.getCategory2Id());
        }

        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            sb.append("category1Id=").append(searchParam.getCategory1Id());
        }

        // 第二个入口：关键词
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            sb.append("keyword").append(searchParam.getKeyword());
        }
        // 在入口的基础上，可以通过品牌的Id，进行过滤
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            if (sb.length()>0){
                sb.append("&trademark=").append(trademark);
            }
        }
        // 在入口的基础上，通过平台属性值 进行过滤
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0){
            for (String prop : props) {
                if (sb.length()>0){
                    sb.append("&props=").append(prop);
                }
            }
        }

        return "list.html?" + sb.toString();
    }
}












































///**
// * author:atGuiGu-mqx
// * date:2022/6/24 10:20
// * 描述：
// **/
//@Controller
//public class ListController {
//
//    @Autowired
//    private ListFeignClient listFeignClient;
//
//    //  全文检索控制器
//    @GetMapping("list.html")
//    public String list(SearchParam searchParam, Model model){
//        //  searchParam urlParam  trademarkParam propsParamList orderMap
//        //  trademarkList attrsList goodsList pageNo totalPages 正好 SearchResponseVo 对象的数据.
//        //  远程调用：
//        Result<Map> result = listFeignClient.list(searchParam);
//
//        //  设置排序：
//        HashMap<String,Object> orderMap = this.makeOrderMap(searchParam.getOrder());
//        //  平台属性面包屑：  实体类 === map
//        //  SearchAttr attrId attrValue attrName
//        List<Map> propsParamList = this.makeProps(searchParam.getProps());
//        //  品牌面包屑：
//        String trademarkParam = this.makeTradeMareParam(searchParam.getTrademark());
//        //  存储urlParam --- 记录用户通过哪些条件进行了检索！
//        String urlParam = this.makeUrlParam(searchParam);
//        model.addAttribute("urlParam",urlParam);
//
//        //  存储排序
//        model.addAttribute("orderMap",orderMap);
//        //  存储平台属性面包屑：
//        model.addAttribute("propsParamList",propsParamList);
//        //  存储品牌面包屑
//        model.addAttribute("trademarkParam",trademarkParam);
//        //  存储的是Map 集合; SearchResponseVo = result.getData(); 实体类与map 是可以互相替换！ 并且feign远程调用的时候底层将数据封装成了LinkedHashMap
//        model.addAllAttributes(result.getData());
//        //  前端查询的条件都会封装到当前这个对象中 searchParam
//        model.addAttribute("searchParam",searchParam);
//        //  返回检索视图页面
//        return "list/index";
//    }
//
//    /**
//     * 设置排序 order=2:desc
//     * @param order
//     * @return
//     */
//    private HashMap<String, Object> makeOrderMap(String order) {
//        //  声明一个对象
//        HashMap<String, Object> hashMap = new HashMap<>();
//        //  判断
//        if (!StringUtils.isEmpty(order)){
//            //  分割字符串
//            String[] split = order.split(":");
//            if (split!=null && split.length==2){
//                hashMap.put("type",split[0]);
//                hashMap.put("sort",split[1]);
//            }
//        }else {
//            //  默认排序规则时给一个默认排序规则
//            hashMap.put("type","1");
//            hashMap.put("sort","desc");
//        }
//        //  返回
//        return hashMap;
//    }
//
//    /**
//     * 制作平台属性面包屑   平台属性名：平台属性值名
//     * @param props
//     * @return
//     */
//    private List<Map> makeProps(String[] props) {
//        //  声明一个集合
//        List<Map> mapList = new ArrayList<>();
//        //  判断
//        if (props!=null && props.length>0){
//            //  循环遍历
//            for (String prop : props) {
//                //  prop = 23:8G:运行内存
//                //  字符串分割
//                String[] split = prop.split(":");
//                if (split!=null && split.length==3){
//                    HashMap<String, Object> hashMap = new HashMap<>();
//                    hashMap.put("attrId",split[0]);
//                    hashMap.put("attrValue",split[1]);
//                    hashMap.put("attrName",split[2]);
//                    //  将面包屑添加到集合中。
//                    mapList.add(hashMap);
//                }
//            }
//        }
//        return mapList;
//    }
//
//    /**
//     * 品牌面包屑
//     * @param trademark
//     * @return
//     */
//    private String makeTradeMareParam(String trademark) {
//        //  判断  trademark=1:小米
//        if (!StringUtils.isEmpty(trademark)){
//            //  分割字符串
//            String[] split = trademark.split(":");
//            if (split!=null && split.length==2){
//                return "品牌："+split[1];
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 记录当前用户通过哪些条件进行了检索
//     * @param searchParam
//     * @return
//     */
//    private String makeUrlParam(SearchParam searchParam) {
//        //  声明一个字符串拼接对象
//        StringBuilder sb = new StringBuilder();
//
//        //  第一个入口： 分类
//        //  http://list.gmall.com/list.html?category3Id=61
//        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
//            sb.append("category3Id=").append(searchParam.getCategory3Id());
//        }
//
//        //  http://list.gmall.com/list.html?category2Id=13
//        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
//            sb.append("category2Id=").append(searchParam.getCategory2Id());
//        }
//
//        //  http://list.gmall.com/list.html?category1Id=2
//        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
//            sb.append("category1Id=").append(searchParam.getCategory1Id());
//        }
//        //  第二个入口： 关键词
//        //  http://list.gmall.com/list.html?keyword=手机
//        if (!StringUtils.isEmpty(searchParam.getKeyword())){
//            sb.append("keyword=").append(searchParam.getKeyword());
//        }
//
//        //  在入口的基础上，可以通过品牌Id 进行过滤.
//        //  http://list.gmall.com/list.html?category3Id=61&trademark=1:小米
//        //  http://list.gmall.com/list.html?keyword=手机&trademark=1:小米
//        String trademark = searchParam.getTrademark();
//        if (!StringUtils.isEmpty(trademark)){
//            if (sb.length()>0){
//                sb.append("&trademark=").append(trademark);
//            }
//        }
//
//        //  在入口的基础上，可以通过平台属性值 进行过滤
//        //  http://list.gmall.com/list.html?category3Id=61&trademark=1:小米&props=23:8G:运行内存&props=24:128G:机身内存
//        String[] props = searchParam.getProps();
//        if (props!=null && props.length>0){
//            for (String prop : props) {
//                if (sb.length()>0){
//                    sb.append("&props=").append(prop);
//                }
//            }
//        }
//        //  返回url 路径
//        return "list.html?"+sb.toString();
//    }
//}
















































//package com.atguigu.gmall.all.controller;
//
//import com.atguigu.gmall.common.result.Result;
//import com.atguigu.gmall.list.client.ListFeignClient;
//import com.atguigu.gmall.model.list.SearchParam;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//

