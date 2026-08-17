package com.met.mto;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@MapperScan("com.met.mto.mapper")
@SpringBootApplication
@EnableScheduling
@Slf4j
public class MtoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtoServerApplication.class, args);
        log.info("Mto启动成功");
    }

}
