package com.tsmc.agenticPortal.sop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agentportal.checkpoint")
public class CheckpointProperties {

    private String type = "mariadb";
    private Mariadb mariadb = new Mariadb();
    private Redis redis = new Redis();

    @Data
    public static class Mariadb {
        private String createOption = "CREATE_IF_NOT_EXISTS";
    }

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String username;
        private String password;
        private int database = 0;
        private long ttl = -1;
    }
}