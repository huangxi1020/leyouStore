package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper  MAPPER =new ObjectMapper();

    public Goods buildGoods(Spu spu) throws Exception {
        Goods goods = new Goods();

        List<String> names = this.categoryClient.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());

        ArrayList<Long> prices = new ArrayList<>();

        List<Map<String,Object>> skuMapList = new ArrayList<>();

        skus.forEach(sku -> {
            prices.add((sku.getPrice()));

            Map<String,Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            map.put("image",StringUtils.isBlank(sku.getImages() )? " " :StringUtils.split(sku.getImages(),",")[0]);

            skuMapList.add(map);
        });

        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), null,true);

        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());

        Map<String ,Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String ,Object>>(){});
        Map<String ,List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String ,List<Object>>>(){});

        Map<String ,Object> specs = new HashMap<>();
        params.forEach(param->{
            if (param.getGeneric()){
                String  value = genericSpecMap.get(param.getId().toString()).toString();
                if (param.getNumeric()){
                  value = chooseSegment(value,param);
                }
                specs.put(param.getName(),value);
            }else {
                List<Object> value = specialSpecMap.get(param.getId().toString());
                specs.put(param.getName(),value);
            }
        });


        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());

        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());

        goods.setPrice(prices);

        goods.setSkus(MAPPER.writeValueAsString(skuMapList));

        goods.setSpecs(specs);


        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {

        if (request==null){
            return null;
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // QueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);

        BoolQueryBuilder basicQuery =  bulidBoolQueryBuilder(request);

        queryBuilder.withQuery(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));

        queryBuilder.withPageable(PageRequest.of(request.getPage()-1,request.getSize()));

        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));

        String categoryAggName = "categories";
        String brandName = "brands";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandName).field("brandId"));

        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

      List<Map<String ,Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands =getBrandAggResult(goodsPage.getAggregation(brandName));

        List<Map<String,Object>> specs=null;

        if (!CollectionUtils.isEmpty(categories) && categories.size()==1){

             specs =  getParamAggResult((Long)categories.get(0).get("id"),basicQuery);

        }

        return new SearchResult(goodsPage.getTotalElements(),goodsPage.getContent(),goodsPage.getTotalPages(),categories,brands,specs);
    }

    private BoolQueryBuilder bulidBoolQueryBuilder(SearchRequest request) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.matchQuery("all",request.getKey()).operator(Operator.AND));

        Map<String, Object> filter = request.getFilter();

        for (Map.Entry<String, Object> entry : filter.entrySet()) {


            String key = entry.getKey();
            if (StringUtils.equals("品牌",key)){
                key = "brandId";
            }else if(StringUtils.equals("分类",key)){
                key = "cid3";
            }else{
                key = "specs." + key + ".keyword";
            }

            boolQueryBuilder.filter(QueryBuilders.termQuery(key,entry.getValue()));

        }

        return boolQueryBuilder;
    }

    private List<Map<String, Object>> getParamAggResult(Long cid, QueryBuilder basicQuery) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        queryBuilder.withQuery(basicQuery);

        List<SpecParam> params = this.specificationClient.queryParams(null, cid,null, true);

        params.forEach(param->{
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });

        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""},null));

        AggregatedPage<Goods> goodsPage = (AggregatedPage)this.goodsRepository.search(queryBuilder.build());

        ArrayList<Map<String ,Object>> specs = new ArrayList<>();

        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();


        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            Map<String ,Object> map = new HashMap<>();
            map.put("k",entry.getKey());

             List<String> options = new ArrayList<>();

            StringTerms terms = (StringTerms)entry.getValue();

            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });

            map.put("options",options);
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {

        LongTerms terms = (LongTerms)aggregation;


     return    terms.getBuckets().stream().map(bucket -> {
            return this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());


    }

    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {

        LongTerms terms = (LongTerms)aggregation;

      return   terms.getBuckets().stream().map(bucket -> {
            HashMap<String , Object> map = new HashMap<>();
            long id = bucket.getKeyAsNumber().longValue();
            List<String> names = this.categoryClient.queryNameByIds(Arrays.asList(id));
            map.put("id",id);
            map.put("names",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }


    public void save(Long id) throws Exception {
        Spu spu = this.goodsClient.querySpuById(id);

        Goods goods = this.buildGoods(spu);

        this.goodsRepository.save(goods);
    }

    public void delete(Long id) {
        this.goodsRepository.deleteById(id);
    }
}
