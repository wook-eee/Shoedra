package com.shop.config;

import com.siot.IamportRestClient.IamportClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamportConfig {

    String apiKey = "0301567788577323";
    String secretKey = "Mb09VW0jQja1Jfv1udKX5SFZxnVwi8STfE7a8KW9RcykVXjWaZk4wYUTebBqWE771M9Anr3su5dRYHq0";


//    @Value("${portone.api.key}")
//    private String apiKey;
//
//    @Value("${portone.api.secret}")
//    private String apiSecret;

    @Bean
    public IamportClient iamportClient() {
        return new IamportClient(apiKey, secretKey);
    }
//    @PostConstruct
//    public void init() {
//        System.out.println("apiKey = " + apiKey);
//        System.out.println("apiSecret = " + apiSecret);
//    }
}
