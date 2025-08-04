package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User user = getUser(userId);
        Event event = getEvent(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new IllegalStateException("Cannot request participation in own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new IllegalStateException("Event is not published");
        }
        if (requestRepository.findByEventAndRequester(event, user).isPresent()) {
            throw new IllegalStateException("Request already exists");
        }
        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new IllegalStateException("Participant limit reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED)
                .build();

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot cancel someone else's request");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        User user = getUser(userId);
        return requestRepository.findAllByRequester(user).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = getEvent(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("Only initiator can view requests for this event");
        }
        return requestRepository.findAllByEvent(event).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto confirmRequest(Long userId, Long eventId, Long reqId) {
        Event event = getEvent(eventId);
        ParticipationRequest request = requestRepository.findById(reqId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("Only initiator can confirm requests");
        }
        if (!request.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Request does not belong to event");
        }
        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new IllegalStateException("Participant limit reached");
        }

        request.setStatus(RequestStatus.CONFIRMED);
        return RequestMapper.toDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto rejectRequest(Long userId, Long eventId, Long reqId) {
        Event event = getEvent(eventId);
        ParticipationRequest request = requestRepository.findById(reqId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("Only initiator can reject requests");
        }
        if (!request.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Request does not belong to event");
        }

        request.setStatus(RequestStatus.REJECTED);
        return RequestMapper.toDto(request);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
    }
}