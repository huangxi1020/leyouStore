package com.leyou.goods.service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;

public class GoodsHtmlService {

    private TemplateEngine engine;

    private GoodsService goodsService;

    public void createHtml(Long spuId){

        Context context = new Context();
        context.setVariables(this.goodsService.loadData(spuId));
        PrintWriter printWriter= null;
        try {
            File file = new File("D:\\nginx-1.14.0\\html\\item" + spuId + ".html");
             printWriter = new PrintWriter(file);

            this.engine.process("iten", context, printWriter);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(printWriter!=null){
                printWriter.close();
            }
        }
    }


    public void deleteHtml(Long id) {
        File file = new File("D:\\nginx-1.14.0\\html\\item" + id + ".html");
        file.deleteOnExit();
    }
}
