package com.atguigu.gmall.product.service;

public interface TestService {
    void testLock();

    // 读锁
    String writeLock();

    //写锁
    String readLock();
}
