package com.leyou.upload.service;

import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;


@Service
public class UploadService {
    private static final List<String> context_types = Arrays.asList("image/gif","image/jpeg");

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {

        String originalFilename = file.getOriginalFilename();

//        StringUtils.substringAfterLast(originalFilename,".");

        String contentType = file.getContentType();

        if (!context_types.contains(contentType)){

            LOGGER.info("文件类型不合法：{}" , originalFilename);
            return null;
        }
    try {
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if (bufferedImage == null) {
            LOGGER.info("文件内容不合法：{}", originalFilename);
        }

//    String ext = StringUtils.substringAfterLast(originalFilename, ".");
//    StorePath storePath = this.storageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);

     file.transferTo(new File("D:\\idea\\com.leyou-parent\\leiyou-upload\\image\\" + originalFilename));
    //return "http://image.leyou.com/" + storePath.getFullPath();
    return "http://image.leyou.com/" + originalFilename;
    }catch (Exception e){

        LOGGER.info("服务器内部错误" +originalFilename);
        e.printStackTrace();
    }
        return null;
        }
}
