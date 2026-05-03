package com.bca.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "fx")
@Data
public class FxConfig {
    private Map<String, BigDecimal> rates = new HashMap<>();
}
