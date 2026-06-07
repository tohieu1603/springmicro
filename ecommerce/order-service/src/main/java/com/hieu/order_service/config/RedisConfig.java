package com.hieu.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public DefaultRedisScript<String> claimIdempotencyScript() {
        var script = new DefaultRedisScript<String>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/claim-idempotency.lua")));
        script.setResultType(String.class);
        return script;
    }
}
