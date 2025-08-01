package ru.practicum.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.repository.StatsRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class StatsServiceImplTest {

    @Mock
    private StatsRepository repository;

    @InjectMocks
    private StatsServiceImpl service;

    private final String start = "2020-01-01 00:00:00";
    private final String end = "2030-01-01 00:00:00";
    private final List<String> uris = List.of("/events");

    @Test
    @DisplayName("getStats: unique = true, uris переданы")
    void getStats_Unique_True_WithUris() {
        when(repository.findAllUniqueStatsWithUris(any(), any(), any()))
                .thenReturn(List.of(new ViewStatsDto("app", "/events", 1L)));

        List<ViewStatsDto> result = service.getStats(start, end, uris, true);

        assertEquals(1, result.size());
        verify(repository).findAllUniqueStatsWithUris(any(), any(), any());
    }

    @Test
    @DisplayName("getStats: unique = false, uris переданы")
    void getStats_Unique_False_WithUris() {
        when(repository.findAllStatsWithUris(any(), any(), any()))
                .thenReturn(List.of(new ViewStatsDto("app", "/events", 5L)));

        List<ViewStatsDto> result = service.getStats(start, end, uris, false);

        assertEquals(1, result.size());
        verify(repository).findAllStatsWithUris(any(), any(), any());
    }

    @Test
    @DisplayName("getStats: unique = true, uris не переданы")
    void getStats_Unique_True_NoUris() {
        when(repository.findAllUniqueStatsWithoutUris(any(), any()))
                .thenReturn(List.of(new ViewStatsDto("app", "/no-uri", 2L)));

        List<ViewStatsDto> result = service.getStats(start, end, null, true);

        assertEquals(1, result.size());
        verify(repository).findAllUniqueStatsWithoutUris(any(), any());
    }

    @Test
    @DisplayName("getStats: unique = false, uris не переданы")
    void getStats_Unique_False_NoUris() {
        when(repository.findAllStatsWithoutUris(any(), any()))
                .thenReturn(List.of(new ViewStatsDto("app", "/none", 3L)));

        List<ViewStatsDto> result = service.getStats(start, end, null, false);

        assertEquals(1, result.size());
        verify(repository).findAllStatsWithoutUris(any(), any());
    }

    @Test
    @DisplayName("saveHit: должен сохранить обращение")
    void saveHit_Should_Save() {
        EndpointHitDto dto = new EndpointHitDto("app", "/uri", "192.168.0.1", "2025-01-01 12:00:00");

        service.saveHit(dto);

        verify(repository).save(argThat(hit ->
                hit.getApp().equals("app") &&
                        hit.getUri().equals("/uri") &&
                        hit.getIp().equals("192.168.0.1")
        ));
    }
}