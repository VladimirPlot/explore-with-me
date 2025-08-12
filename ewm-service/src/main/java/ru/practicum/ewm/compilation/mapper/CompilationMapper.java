package ru.practicum.ewm.compilation.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;

import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation) {
        Set<EventShortDto> events = compilation.getEvents().stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toSet());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events)
                .build();
    }
}