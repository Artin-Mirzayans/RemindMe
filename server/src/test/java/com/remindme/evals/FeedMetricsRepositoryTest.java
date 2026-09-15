package com.remindme.evals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@ExtendWith(MockitoExtension.class)
class FeedMetricsRepositoryTest {

    private static final LocalDate DAY = LocalDate.parse("2030-01-10");

    @Mock
    private DynamoDbClient dynamoDbClient;

    private FeedMetricsRepository repository() {
        return new FeedMetricsRepository(dynamoDbClient);
    }

    @Test
    @DisplayName("a request increments the day's Requests counter and stamps a TTL")
    void recordRequestIncrementsCounter() {
        repository().recordRequest("Digest", DAY);

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbClient).updateItem(captor.capture());

        UpdateItemRequest request = captor.getValue();
        assertThat(request.key().get("Feature").s()).isEqualTo("Digest");
        assertThat(request.key().get("Day").s()).isEqualTo("2030-01-10");
        assertThat(request.updateExpression()).contains("ADD Requests");
        assertThat(request.updateExpression()).contains("SET #ttl = :ttl");
    }

    @Test
    @DisplayName("a generation increments Generations plus the matching outcome bucket")
    void recordGenerationIncrementsOutcomeBucket() {
        repository().recordGeneration("Watchlist", DAY, GenerationOutcome.EMPTY, 0.02, 10, 20, 300);

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbClient).updateItem(captor.capture());

        // field order isn't guaranteed (Map.of shuffles it per JVM run), just that both landed
        assertThat(captor.getValue().updateExpression()).startsWith("ADD ").contains("Generations")
                .contains("Empties");
    }

    @Test
    @DisplayName("does not throw when DynamoDB fails - a metrics hiccup shouldn't break a feed")
    void swallowsWriteFailures() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(new RuntimeException("boom"));

        repository().recordRequest("Digest", DAY);
    }

    @Test
    @DisplayName("maps a query response back into DailyMetrics")
    void findRangeMapsResults() {
        QueryResponse response = QueryResponse.builder()
                .items(List.of(Map.of(
                        "Feature", AttributeValue.fromS("Digest"),
                        "Day", AttributeValue.fromS("2030-01-10"),
                        "Requests", AttributeValue.fromN("5"),
                        "Generations", AttributeValue.fromN("1"),
                        "Successes", AttributeValue.fromN("1"),
                        "CostUsd", AttributeValue.fromN("0.0594"))))
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        List<DailyMetrics> found = repository().findRange("Digest", DAY, DAY);

        assertThat(found).hasSize(1);
        DailyMetrics metrics = found.get(0);
        assertThat(metrics.requests()).isEqualTo(5);
        assertThat(metrics.generations()).isEqualTo(1);
        assertThat(metrics.costUsd()).isEqualTo(0.0594);
        assertThat(metrics.empties()).isZero();
    }

    @Test
    @DisplayName("returns an empty list rather than throwing when the query fails")
    void findRangeSwallowsFailures() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(new RuntimeException("boom"));

        assertThat(repository().findRange("Digest", DAY, DAY)).isEmpty();
    }
}
