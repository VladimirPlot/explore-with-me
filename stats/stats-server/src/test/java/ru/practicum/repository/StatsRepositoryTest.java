package ru.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StatsRepositoryTest {

    @Autowired
    private StatsRepository repository;

    @Test
    void testSaveAndQuery() {
        repository.save(new EndpointHit(null, "test-app", "/uri", "127.0.0.1",
                LocalDateTime.of(2025, 7, 15, 12, 0)));

        List<?> stats = repository.findAllStatsWithoutUris(
                LocalDateTime.of(2025, 7, 1, 0, 0),
                LocalDateTime.of(2025, 7, 30, 0, 0));

        assertThat(stats).hasSize(1);
    }
}