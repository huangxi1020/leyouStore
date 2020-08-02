package com.leyou.item.api;

import com.leyou.common.pojo.PageRseult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GoodsApi {
    @GetMapping("spu/detail/{spuId}")
    public SpuDetail querySpuDetailBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("spu/page")
    public PageRseult<SpuBo> querySpuBoPage(

            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "saleable",required = false)Boolean saleable,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows
    );

    @GetMapping("sku/list")
    public List<Sku> querySkusBySpuId(@RequestParam("id") Long spuId);

    @GetMapping("{id}")
    public Spu querySpuById(@PathVariable("id") Long id);

    @GetMapping("sku/{skuId}")
    public Sku querySkuBySkuId(@PathVariable("skuId") Long skuId);
}
