package com.lxe.lx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 可以直接使用addResourceLocations 指定磁盘绝对路径，同样可以配置多个位置，注意路径写法需要加上file:
//        registry.addResourceHandler("/uploadFilesTest/file/**").addResourceLocations("file:/home/server/eaiap/uploadFilesTest/file/");
//        registry.addResourceHandler("/uploadFilesTest/file/**").addResourceLocations("file:D:/uploadFilesTest/file/");
    }

}