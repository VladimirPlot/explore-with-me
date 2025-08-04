package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
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

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventServiceImplAdminSearchTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Category cat1;
    private Category cat2;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().withNano(0);

        user = userRepository.save(new User(null, "Admin", "admin@example.com"));
        cat1 = categoryRepository.save(new Category(null, "Tech"));
        cat2 = categoryRepository.save(new Category(null, "Music"));

        eventRepository.save(Event.builder()
                .title("Java Meetup")
                .annotation("Java devs")
                .description("Description here")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PENDING)
                .paid(true)
                .participantLimit(10)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat1)
                .build());

        eventRepository.save(Event.builder()
                .title("Jazz Night")
                .annotation("Jazz music")
                .description("Smooth jazz")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PENDING)
                .paid(false)
                .participantLimit(50)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat2)
                .build());

        eventRepository.save(Event.builder()
                .title("Spring Boot Conf")
                .annotation("Spring devs")
                .description("Spring all day")
                .eventDate(now.plusDays(5))
                .createdOn(now)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PUBLISHED)
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .location(new Location(1.0, 2.0))
                .initiator(user)
                .category(cat1)
                .build());
    }

    @Test
    void findAllAdmin_shouldReturnAllPendingEventsByDefault() {
        List<EventFullDto> result = eventService.findAllAdmin(
                null, null, null,
                now.toString(), now.plusDays(10).toString(),
                EventSort.EVENT_DATE, 0, 10);

        assertEquals(2, result.size());
    }

    @Test
    void findAllAdmin_shouldFilterByCategoryAndPaid() {
        List<EventFullDto> result = eventService.findAllAdmin(
                null,
                List.of(cat2.getId()),
                false,
                now.toString(), now.plusDays(10).toString(),
                EventSort.EVENT_DATE, 0, 10);

        assertEquals(1, result.size());
        assertEquals("Jazz Night", result.get(0).getTitle());
    }

    @Test
    void findAllAdmin_shouldFilterByText() {
        List<EventFullDto> result = eventService.findAllAdmin(
                "Java", null, null,
                now.toString(), now.plusDays(10).toString(),
                EventSort.EVENT_DATE, 0, 10);

        assertEquals(1, result.size());
        assertEquals("Java Meetup", result.get(0).getTitle());
    }

    @Test
    void findAllAdmin_shouldFilterByDateRange() {
        String rangeStart = LocalDateTime.now().plusDays(1).toString();
        String rangeEnd = LocalDateTime.now().plusDays(2).toString();

        List<EventFullDto> result = eventService.findAllAdmin(
                null, null, null,
                rangeStart, rangeEnd,
                EventSort.EVENT_DATE, 0, 10);

        assertEquals(0, result.size());
    }
}