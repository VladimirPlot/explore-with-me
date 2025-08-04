package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByInitiator(User initiator, Pageable pageable);

    List<Event> findAllByIdIn(List<Long> ids);

    List<Event> findAllByCategoryId(Long categoryId);

    List<Event> findAllByState(EventState state, Pageable pageable);

    List<Event> findAllByEventDateAfterAndStateAndCategoryIdInAndPaidIn(
            LocalDateTime rangeStart,
            EventState state,
            List<Long> categoryIds,
            List<Boolean> paid,
            Pageable pageable
    );

    List<Event> findAllByEventDateBetweenAndState(
            LocalDateTime start,
            LocalDateTime end,
            EventState state,
            Pageable pageable
    );
}