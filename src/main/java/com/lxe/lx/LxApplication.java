package com.lxe.lx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lxe.lx.mapper")
public class LxApplication {

    public static void main(String[] args) {
        SpringApplication.run(LxApplication.class, args);
    }

}
