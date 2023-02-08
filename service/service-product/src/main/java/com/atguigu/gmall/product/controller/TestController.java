package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;
    
    @GetMapping("testLock")
    public Result testLock(){
        testService.testLock();
        return Result.ok();
    }

    //写锁
    @GetMapping("write")
    public Result<String> write(){
        String msg = testService.writeLock();
        return Result.ok(msg);
    }


    //读锁
    @GetMapping("read")
    public Result readLock(){
        String msg = testService.readLock();
        return Result.ok(msg);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture<Void> hello = CompletableFuture.runAsync(() -> {
//            System.out.println("hello");
//        });
//
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            return 1024;
//        });
//
//        System.out.println(hello.get());
//        System.out.println(integerCompletableFuture.get());

//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            int i = 1/0;
//            return 1024;
//        }).whenComplete((i , t) -> {
//            System.out.println("i: \t" + i);
//            System.out.println("t: \t" + t);
//        }).exceptionally((r) -> {
//            System.out.println("r: \t" + r);
//            return 404;
//        });
//        System.out.println(integerCompletableFuture.get());

        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
            return 1024;
        }).thenApply(integer -> {
            return 2 * integer;
        }).whenComplete((i , t) -> {
            System.out.println("i: \t" + i);
            System.out.println("t: \t" + t);
        }).exceptionally((r) -> {
            System.out.println("r: \t" + r);
            return 404;
        });
        System.out.println(integerCompletableFuture.get());
        //thenAccept 没有返回值！
        CompletableFuture.supplyAsync(() -> {
            return 1024;
        }).thenAccept(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {

            }
        });
    }
}


































