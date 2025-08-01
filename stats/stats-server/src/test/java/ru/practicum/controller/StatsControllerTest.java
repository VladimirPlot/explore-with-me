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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void saveHit_shouldReturn201() throws Exception {
        EndpointHitDto dto = new EndpointHitDto("test-app", "/test", "127.0.0.1", "2025-07-29 18:45:00");

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void getStats_shouldReturnList() throws Exception {
        ViewStatsDto stat = new ViewStatsDto("test-app", "/uri", 10L);
        Mockito.when(service.getStats(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(List.of(stat));

        mockMvc.perform(get("/stats")
                        .param("start", "2025-07-01 00:00:00")
                        .param("end", "2025-07-30 00:00:00")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].hits").value(10));
    }
}