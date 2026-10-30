package com.collins.marketdataservice.configs;

import com.collins.marketdataservice.payloads.StockPriceResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, StockPriceResponse> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, StockPriceResponse> template =
                new RedisTemplate<>();

        StringRedisSerializer stringSerializer =
                new StringRedisSerializer();

        JacksonJsonRedisSerializer<StockPriceResponse> jsonSerializer =
                new JacksonJsonRedisSerializer<>(
                        JsonMapper.builder().build(),
                        StockPriceResponse.class
                );

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }
}