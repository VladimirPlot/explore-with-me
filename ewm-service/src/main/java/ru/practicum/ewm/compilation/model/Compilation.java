package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compilation_seq")
    @SequenceGenerator(name = "compilation_seq", sequenceName = "compilation_sequence", allocationSize = 1)
    private Long id;

    private String title;

    private Boolean pinned;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<ru.practicum.ewm.event.model.Event> events;
}