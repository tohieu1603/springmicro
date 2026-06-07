package com.hieu.inventory_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/** Redis configuration — loads Lua script for atomic stock reservation. */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public DefaultRedisScript<Long> reserveStockScript() {
        var script = new DefaultRedisScript<Long>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/reserve-stock.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /** Lua script for atomic batch stock release — single round-trip prevents partial rollback. */
    @Bean
    public DefaultRedisScript<Long> releaseStockBatchScript() {
        var script = new DefaultRedisScript<Long>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release-stock-batch.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
