package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageRseult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageRseult<SpuBo> querySpuByPage(String key, Boolean saleable, Integer page , Integer rows) {

        Example example = new  Example(Spu.class);

        Example.Criteria criteria = example.createCriteria();

        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }

        if (saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }

        PageHelper.startPage(page, rows);


        List<Spu> spus = this.spuMapper.selectByExample(example);

        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        List<SpuBo> spuBos = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();

            BeanUtils.copyProperties(spu, spuBo);

            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());

            List<String> names = this.categoryService.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(),spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "-"));
            return spuBo;
        }).collect(Collectors.toList());


        return new PageRseult<>(pageInfo.getTotal(),spuBos);
    }


    @Transactional
    public void saveGoods(SpuBo spuBo) {

        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        this.spuMapper.insertSelective(spuBo);

        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        this.spuDetailMapper.insertSelective(spuDetail);

        saveSkuAndStock(spuBo);
        sengMsg("insert",spuBo.getId());

    }

    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(
                sku -> {
                    sku.setId(null);
                    sku.setSpuId(spuBo.getId());
                    sku.setCreateTime(new Date());
                    sku.setLastUpdateTime(sku.getCreateTime());
                    this.skuMapper.insertSelective(sku);

                    Stock stock = new Stock();
                    stock.setSkuId(sku.getId());
                    stock.setStock(sku.getStock());
                    this.stockMapper.insertSelective(stock);

                }
        );
        sengMsg("update",spuBo.getId());
    }

    private void sengMsg(String  type,Long id) {
        try{
                this.amqpTemplate.convertAndSend("item." + type,id);
            }catch (Exception e){
            e.printStackTrace();
        }
    }



    public SpuDetail querySpuDetailBySpuId(Long spuId) {
     return this.spuDetailMapper.selectByPrimaryKey(spuId);
    }

    public List<Sku> querySkusBySpuId(Long spuId) {

        Sku record = new Sku();

        record.setSpuId(spuId);

        List<Sku> skus = this.skuMapper.select(record);

        skus.forEach(sku -> {
            Stock stock = this.stockMapper.selectByPrimaryKey(sku.getId());
            sku.setStock(stock.getStock());
        });

        return skus;
    }

    @Transactional
    public void updateGoods(SpuBo spuBo) {
        Sku record = new Sku();

        record.setSpuId(spuBo.getId());

        List<Sku> skus = this.skuMapper.select(record);

        skus.forEach(sku -> {
            this.stockMapper.deleteByPrimaryKey(sku.getId());
        });

        Sku sku1 = new Sku();
        sku1.setSpuId(spuBo.getId());
        this.skuMapper.delete(sku1);

        saveSkuAndStock(spuBo);

        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        spuBo.setSaleable(null);
        spuBo.setValid(null);
        this.spuMapper.updateByPrimaryKeySelective(spuBo);

        this.spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());


    }

    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    public Sku querySkuBySkuId(Long skuId) {
        return this.skuMapper.selectByPrimaryKey(skuId);
    }
}
