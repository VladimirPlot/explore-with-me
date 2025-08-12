package ru.practicum.ewm.request.mapper;

import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;

public class RequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .created(request.getCreated())
                .status(request.getStatus())
                .build();
    }
}