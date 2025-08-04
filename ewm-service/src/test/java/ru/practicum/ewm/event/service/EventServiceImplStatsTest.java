package ru.practicum.ewm.event.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventServiceImplStatsTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private StatsClient statsClient;

    private Event event;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User(null, "StatsUser", "stats@example.com"));
        Category category = categoryRepository.save(new Category(null, "Category"));

        event = eventRepository.save(Event.builder()
                .title("Public Event")
                .annotation("Ann")
                .description("Desc")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PUBLISHED)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(category)
                .build());
    }

    @Test
    void getPublicEvent_shouldReturnEventWithViews() {
        ViewStatsDto statsDto = new ViewStatsDto("ewm-service", "/events/" + event.getId(), 42L);
        when(statsClient.getStats(any(), any(), any(), eq(true)))
                .thenReturn(List.of(statsDto));

        EventFullDto dto = eventService.getPublicEvent(event.getId(), "192.0.2.1", "/events/" + event.getId());

        assertEquals(event.getId(), dto.getId());
        assertEquals(42, dto.getViews());

        verify(statsClient, times(1)).hit(any());
        verify(statsClient, times(1)).getStats(any(), any(), any(), eq(true));
    }

    @Test
    void getPublicEvent_shouldThrowIfNotPublished() {
        event.setState(EventState.PENDING);
        eventRepository.save(event);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> eventService.getPublicEvent(event.getId(), "192.0.2.1", "/events/" + event.getId()));

        assertEquals("Event is not published", ex.getMessage());
        verify(statsClient, never()).hit(any());
        verify(statsClient, never()).getStats(any(), any(), any(), anyBoolean());
    }
}