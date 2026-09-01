package com.tju.elm_bk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.tju.elm_bk.mapper", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class ElmBkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElmBkApplication.class, args);
    }

}
