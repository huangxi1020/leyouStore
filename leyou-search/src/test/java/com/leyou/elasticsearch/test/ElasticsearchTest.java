package com.leyou.elasticsearch.test;

import com.leyou.common.pojo.PageRseult;
import com.leyou.item.bo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticsearchTest {


    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GoodsClient goodsClient;

    @Test
    public void test(){

        this.elasticsearchTemplate.createIndex(Goods.class);
        this.elasticsearchTemplate.putMapping(Goods.class);

        Integer page = 1;
        Integer rows = 100;

        do {
            PageRseult<SpuBo> rseult = this.goodsClient.querySpuBoPage(null, null, page, rows);

            List<SpuBo> items = rseult.getItems();

            List<Goods> goodsList = items.stream().map(spuBo -> {
                try {
                    return this.searchService.buildGoods(spuBo);
                } catch (Exception e) {
                    e.printStackTrace();

                }
                return null;
            }).collect(Collectors.toList());

            this.goodsRepository.saveAll(goodsList);

            rows = items.size();
            page++;
        }while (rows == 100);

    }
}
