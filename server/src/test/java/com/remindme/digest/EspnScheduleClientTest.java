package com.remindme.digest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class EspnScheduleClientTest {

    private final EspnScheduleClient client = new EspnScheduleClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SCOREBOARD = """
            {"leagues":[{"name":"UEFA Champions League"}],"events":[
              {"name":"AS Roma at Fenerbahce","date":"2026-09-10T16:45Z",
               "status":{"type":{"state":"in"}},
               "competitions":[{"broadcasts":[{"market":"national","names":["Paramount+"]}]}]},
              {"name":"Bodo/Glimt at Bayern Munich","date":"2026-09-10T19:00Z",
               "status":{"type":{"state":"pre"}},
               "competitions":[{"broadcasts":[{"market":"national","names":["Paramount+","CBS"]}]}]}
            ]}""";

    @Test
    @DisplayName("keeps only upcoming fixtures, normalizes the timestamp, and captures the broadcaster")
    void parsesUpcomingFixtures() throws Exception {
        List<EspnScheduleClient.Fixture> fixtures = client.parseScoreboard(mapper.readTree(SCOREBOARD),
                "soccer/uefa.champions");

        assertThat(fixtures).hasSize(1);
        EspnScheduleClient.Fixture f = fixtures.get(0);
        assertThat(f.matchup()).isEqualTo("Bodo/Glimt at Bayern Munich");
        assertThat(f.startsAt()).isEqualTo("2026-09-10T19:00:00Z");
        assertThat(f.competition()).isEqualTo("UEFA Champions League");
        assertThat(f.watch()).isEqualTo("Paramount+, CBS");
        assertThat(f.line()).isEqualTo(
                "Bodo/Glimt at Bayern Munich | 2026-09-10T19:00:00Z | UEFA Champions League | Paramount+, CBS");
    }

    @Test
    @DisplayName("for MLB, drops games without a national broadcast")
    void mlbKeepsOnlyNationalGames() throws Exception {
        String mlb = """
                {"leagues":[{"name":"Major League Baseball"}],"events":[
                  {"name":"Rays at Braves","date":"2026-09-10T23:05Z","status":{"type":{"state":"pre"}},
                   "competitions":[{"broadcasts":[{"market":"local","names":["Bally Sports"]}]}]},
                  {"name":"Yankees at Red Sox","date":"2026-09-10T23:10Z","status":{"type":{"state":"pre"}},
                   "competitions":[{"broadcasts":[{"market":"national","names":["ESPN"]}]}]}
                ]}""";

        List<EspnScheduleClient.Fixture> fixtures = client.parseScoreboard(mapper.readTree(mlb), "baseball/mlb");

        assertThat(fixtures).extracting(EspnScheduleClient.Fixture::matchup).containsExactly("Yankees at Red Sox");
    }
}
