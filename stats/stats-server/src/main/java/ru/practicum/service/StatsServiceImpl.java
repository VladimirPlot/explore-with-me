package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();
        statsRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start must be before end");
        }
        if (uris != null && !uris.isEmpty()) {
            return unique
                    ? statsRepository.findAllUniqueStatsWithUris(start, end, uris)
                    : statsRepository.findAllStatsWithUris(start, end, uris);
        }
        return unique
                ? statsRepository.findAllUniqueStatsWithoutUris(start, end)
                : statsRepository.findAllStatsWithoutUris(start, end);
    }
}