package ru.practicum.ewm.compilation.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompilationMapperTest {

    @Test
    void toDto_shouldMapAllFieldsCorrectly() {
        User initiator = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@mail.com")
                .build();

        Category category = Category.builder()
                .id(2L)
                .name("Music")
                .build();

        Event event = Event.builder()
                .id(3L)
                .title("Jazz Night")
                .annotation("Evening of Jazz")
                .description("Smooth jazz music")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .location(new Location(10.0, 20.0))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0L)
                .views(0L)
                .initiator(initiator)
                .category(category)
                .build();

        Compilation compilation = Compilation.builder()
                .id(10L)
                .title("Best of Music")
                .pinned(true)
                .events(Set.of(event))
                .build();

        CompilationDto dto = CompilationMapper.toDto(compilation);

        assertEquals(compilation.getId(), dto.getId());
        assertEquals(compilation.getTitle(), dto.getTitle());
        assertEquals(compilation.getPinned(), dto.getPinned());
        assertNotNull(dto.getEvents());
        assertEquals(1, dto.getEvents().size());

        EventShortDto mappedEvent = dto.getEvents().iterator().next();
        assertEquals(event.getId(), mappedEvent.getId());
        assertEquals(event.getTitle(), mappedEvent.getTitle());
        assertEquals(event.getAnnotation(), mappedEvent.getAnnotation());
    }
}