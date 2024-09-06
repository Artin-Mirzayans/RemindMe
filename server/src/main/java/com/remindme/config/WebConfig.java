package com.remindme.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.remindme.filters.TokenValidationFilter;

@Configuration
public class WebConfig {

    @Value("${cors.allowed_origins}")
    private String[] allowedOrigins;

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(1);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TokenValidationFilter> tokenValidationFilterRegistration(Environment environment) {
        FilterRegistrationBean<TokenValidationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TokenValidationFilter(environment));
        registration.addUrlPatterns("/reminders/*", "/users/*", "/otp/*");
        registration.setOrder(2);
        return registration;
    }
}
