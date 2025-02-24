package com.lxe.lx.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class RedisTestController {
    private final StringRedisTemplate redisTemplate;

    public RedisTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/set")
    public String setValue() {
        redisTemplate.opsForValue().set("testKey", "Hello Redis!");
        return "Redis 存储成功";
    }

    @GetMapping("/get")
    public String getValue() {
        return redisTemplate.opsForValue().get("testKey");
    }
}
