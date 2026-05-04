package com.taxi.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
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

    /**
     * RestTemplate bean for external API calls with increased timeouts for Claude API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Connection timeout: 5 minutes for establishing connection
        factory.setConnectTimeout(300000);
        // Read timeout: 10 minutes for waiting on response (Claude can take time for multi-page PDFs)
        factory.setReadTimeout(600000);
        factory.setBufferRequestBody(true);
        return new BufferingClientHttpRequestFactory(factory);
    }
}
