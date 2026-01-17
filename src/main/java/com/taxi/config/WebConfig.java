package com.taxi.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Increase async request timeout to 30 minutes for large CSV uploads
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 30 minutes timeout for async requests
        configurer.setDefaultTimeout(Duration.ofMinutes(30).toMillis());
    }

    /**
     * Configure Tomcat for longer timeouts
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // Connection timeout: 30 minutes
                connector.setProperty("connectionTimeout", "1800000");
                // Keep-alive timeout
                connector.setProperty("keepAliveTimeout", "1800000");
            });
        };
    }
}
