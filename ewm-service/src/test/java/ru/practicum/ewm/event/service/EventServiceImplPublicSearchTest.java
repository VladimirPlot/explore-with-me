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
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
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
class EventServiceImplPublicSearchTest {

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

    private Event publishedEvent;

    @BeforeEach
    void setup() {
        User user = userRepository.save(new User(null, "PublicUser", "pub@example.com"));
        Category cat = categoryRepository.save(new Category(null, "Tech"));

        publishedEvent = eventRepository.save(Event.builder()
                .title("JavaConf")
                .annotation("Conf")
                .description("Java conference")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PUBLISHED)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat)
                .build());
    }

    @Test
    void findPublic_shouldReturnFilteredPublishedEvents() {
        doNothing().when(statsClient).hit(any());

        List<EventShortDto> result = eventService.findPublic(
                "Java",
                List.of(publishedEvent.getCategory().getId()),
                true,
                null,
                null,
                false,
                EventSort.EVENT_DATE,
                0,
                10,
                "192.0.2.1",
                "/events/search"
        );

        assertEquals(1, result.size());
        assertEquals("JavaConf", result.get(0).getTitle());

        verify(statsClient, times(1)).hit(any());
    }

    @Test
    void findPublic_shouldReturnEmpty_whenTextDoesNotMatch() {
        doNothing().when(statsClient).hit(any());

        List<EventShortDto> result = eventService.findPublic(
                "Python",
                null, null, null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");

        assertTrue(result.isEmpty());
    }

    @Test
    void findPublic_shouldFilterByAvailability() {
        publishedEvent.setParticipantLimit(0);
        eventRepository.save(publishedEvent);

        doNothing().when(statsClient).hit(any());

        List<EventShortDto> result = eventService.findPublic(
                null, null, null, null, null,
                true, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");

        assertEquals(1, result.size());
    }
}