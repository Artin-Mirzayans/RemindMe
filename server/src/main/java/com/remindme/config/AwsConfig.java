package com.remindme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.regions.Region;

@Configuration
public class AwsConfig {

    @Bean
    DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.US_WEST_1)
                .build();
    }

    @Bean
    SchedulerClient schedulerClient() {
        return SchedulerClient.builder()
                .region(Region.US_WEST_1)
                .build();
    }

    @Bean
    PinpointSmsVoiceV2Client pinpointClient() {
        return PinpointSmsVoiceV2Client.builder()
                .region(Region.US_WEST_1)
                .build();
    }
}
