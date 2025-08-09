package ru.practicum.ewm.request.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RequestMapperTest {

    @Test
    void toDto_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .build();

        Event event = Event.builder()
                .id(2L)
                .title("Event")
                .initiator(user)
                .build();

        LocalDateTime created = LocalDateTime.now();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(3L)
                .event(event)
                .requester(user)
                .created(created)
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequestDto dto = RequestMapper.toDto(request);

        assertEquals(3L, dto.getId());
        assertEquals(2L, dto.getEvent());
        assertEquals(1L, dto.getRequester());
        assertEquals(created, dto.getCreated());
        assertEquals(RequestStatus.CONFIRMED, dto.getStatus());
    }
}