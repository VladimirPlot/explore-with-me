package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService service;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void saveHit_shouldReturn201() throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.parse("2025-07-29 18:45:00", FORMATTER))
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        Mockito.verify(service).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void getStats_shouldReturnList() throws Exception {
        ViewStatsDto stat = new ViewStatsDto("test-app", "/uri", 10L);
        Mockito.when(service.getStats(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        isNull(),               // uris в запросе не передаём
                        eq(false)))
                .thenReturn(List.of(stat));

        mockMvc.perform(get("/stats")
                        .param("start", "2025-07-01 00:00:00")
                        .param("end", "2025-07-30 00:00:00")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].hits").value(10));

        Mockito.verify(service).getStats(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                eq(false)
        );
    }

    @Test
    void getStats_shouldReturn400_whenBadDateFormat() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2025/07/01 00:00:00") // неверный формат
                        .param("end", "2025-07-30 00:00:00"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(service);
    }

    @Test
    void getStats_shouldReturn400_whenStartAfterEnd() throws Exception {
        Mockito.when(service.getStats(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(), anyBoolean()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "start must be before end"));

        mockMvc.perform(get("/stats")
                        .param("start", "2035-07-01 00:00:00")
                        .param("end", "2020-07-30 00:00:00"))
                .andExpect(status().isBadRequest());

        Mockito.verify(service).getStats(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull(),
                eq(false));
    }
}