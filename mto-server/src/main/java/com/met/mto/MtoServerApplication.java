package com.met.mto;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@MapperScan("com.met.mto.mapper")
@SpringBootApplication
@Slf4j
public class MtoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtoServerApplication.class, args);
        log.info("Mto启动成功");
    }

}
