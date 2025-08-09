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

    @Test
    void findPublic_shouldThrow_whenRangeStartAfterRangeEnd() {
        assertThrows(RuntimeException.class, () -> eventService.findPublic(
                null, null, null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search"
        ));
    }

    @Test
    void findPublic_shouldIgnoreEmptyCategoriesList() {
        doNothing().when(statsClient).hit(any());

        List<EventShortDto> result = eventService.findPublic(
                null, List.of(), null, null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search"
        );

        assertEquals(1, result.size());
        assertEquals(publishedEvent.getId(), result.get(0).getId());
    }

    @Test
    void findPublic_shouldFilterByPaid() {
        doNothing().when(statsClient).hit(any());

        var paidOnly = eventService.findPublic(
                null, null, true, null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");
        assertEquals(1, paidOnly.size());

        var freeOnly = eventService.findPublic(
                null, null, false, null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");
        assertTrue(freeOnly.isEmpty());
    }

    @Test
    void findPublic_shouldExcludeWhenLimitReached_withOnlyAvailable() {
        publishedEvent.setParticipantLimit(1);
        publishedEvent.setConfirmedRequests(1L);
        eventRepository.save(publishedEvent);

        doNothing().when(statsClient).hit(any());

        var result = eventService.findPublic(
                null, null, null, null, null,
                true, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");

        assertTrue(result.isEmpty());
    }

    @Test
    void findPublic_shouldNotReturnPastEvents_whenNoRangeProvided() {
        User u = userRepository.findAll().get(0);
        Category c = categoryRepository.findAll().get(0);
        eventRepository.save(Event.builder()
                .title("Past")
                .annotation("past")
                .description("was yesterday")
                .eventDate(LocalDateTime.now().minusDays(1))
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L).views(0L)
                .state(EventState.PUBLISHED)
                .paid(false).participantLimit(0).requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(u).category(c)
                .build());

        doNothing().when(statsClient).hit(any());

        var result = eventService.findPublic(
                null, null, null,
                null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");

        assertEquals(1, result.size());
        assertEquals(publishedEvent.getId(), result.get(0).getId());
    }

    @Test
    void findPublic_shouldSortByViewsDesc() {
        User u = userRepository.findAll().get(0);
        Category c = categoryRepository.findAll().get(0);

        Event low = eventRepository.save(Event.builder()
                .title("LowViews")
                .annotation("a").description("d")
                .eventDate(LocalDateTime.now().plusDays(2))
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L).views(1L)
                .state(EventState.PUBLISHED)
                .paid(false).participantLimit(0).requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(u).category(c)
                .build());

        Event high = eventRepository.save(Event.builder()
                .title("HighViews")
                .annotation("a").description("d")
                .eventDate(LocalDateTime.now().plusDays(3))
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L).views(5L)
                .state(EventState.PUBLISHED)
                .paid(false).participantLimit(0).requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(u).category(c)
                .build());

        doNothing().when(statsClient).hit(any());

        var result = eventService.findPublic(
                null, null, null, null, null,
                false, EventSort.VIEWS, 0, 10,
                "192.0.2.1", "/events/search");

        assertFalse(result.isEmpty());
        assertEquals("HighViews", result.get(0).getTitle());
    }

    @Test
    void findPublic_shouldNotMatchTitle_whenTextSearch() {
        doNothing().when(statsClient).hit(any());

        var byTitle = eventService.findPublic(
                publishedEvent.getTitle(), // "JavaConf"
                null, null, null, null,
                false, EventSort.EVENT_DATE, 0, 10,
                "192.0.2.1", "/events/search");

        assertTrue(byTitle.isEmpty());
    }
}