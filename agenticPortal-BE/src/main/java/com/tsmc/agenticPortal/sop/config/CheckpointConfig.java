package com.tsmc.agenticPortal.sop.config;

import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.CreateOption;
import org.bsc.langgraph4j.checkpoint.MysqlSaver;
import org.bsc.langgraph4j.checkpoint.RedisSaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
public class CheckpointConfig {

    @Bean
    public BaseCheckpointSaver checkpointSaver(CheckpointProperties properties, DataSource dataSource) {
        String type = properties.getType();

        if ("redis".equalsIgnoreCase(type)) {
            CheckpointProperties.Redis redis = properties.getRedis();

            RedisSaver.Builder builder = RedisSaver.builder()
                    .host(redis.getHost())
                    .port(redis.getPort())
                    .database(redis.getDatabase())
                    .ttl(redis.getTtl(), TimeUnit.MINUTES);

            if (redis.getUsername() != null && !redis.getUsername().isBlank()) {
                builder.username(redis.getUsername());
            }
            if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
                builder.password(redis.getPassword());
            }

            return builder.build();
        }

        CheckpointProperties.Mariadb mariadb = properties.getMariadb();

        return MysqlSaver.builder()
                .dataSource(dataSource)
                .createOption(CreateOption.valueOf(mariadb.getCreateOption()))
                .build();
    }
}