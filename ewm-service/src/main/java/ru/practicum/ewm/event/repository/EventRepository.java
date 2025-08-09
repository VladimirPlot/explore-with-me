package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByInitiator(User initiator, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("""
            SELECT e FROM Event e
            WHERE e.state = :state
              AND e.eventDate BETWEEN :rangeStart AND :rangeEnd
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
            """)
    Page<Event> findPublicEvents(@Param("state") EventState state,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 Pageable pageable);
}