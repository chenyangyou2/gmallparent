package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/product/")
@RefreshScope
public class FileUploadController {
    //  将服务器地址，用户名，密码等放入配置文件中 采用软编码形式存储。
    @Value("${minio.endpointUrl}")
    private String endpoint;    //  endpoint=http://192.168.200.129:9000
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secreKey}")
    private String secreKey;
    @Value("${minio.bucketName}")  //  桶发生了变化! 修改 nacos 配置文件.
    private String bucketName;

    //  文件上传控制器 springmvc 文件上传的知识：
    //  /admin/product/fileUpload
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file){
        //  声明一个url 地址
        String url="";
        try {
            //  1.  创建一个客户端：
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(accessKey, secreKey)
                            .build();

            //  2.  创建桶：存储文件的地方。
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket 'gmall' already exists.");
            }

            //  3.  上传文件：
            String fileName = System.currentTimeMillis() + UUID.randomUUID().toString();
            // Upload known sized input stream.
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                            file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            //  http://img10.360buyimg.com/n7/jfs/t1/113960/3/20088/56974/5f861926E5153a0ef/1831cb31ecb63f24.jpg
            //  endpoint=http://192.168.200.129:9000/gmall/
            url = endpoint + "/"+bucketName+"/" + fileName;

            System.out.println("url:\t"+url);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }
        //  返回数据.
        return Result.ok(url);
    }
}