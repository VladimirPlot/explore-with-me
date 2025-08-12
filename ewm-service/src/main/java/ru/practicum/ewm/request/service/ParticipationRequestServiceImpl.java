package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.RequestStatusUpdateDto;
import ru.practicum.ewm.request.dto.RequestStatusUpdateResult;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private void requireInitiator(Event event, Long userId, String action) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only initiator can " + action);
        }
    }

    private void refreshConfirmedCount(Event event) {
        long confirmed = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        event.setConfirmedRequests(confirmed);
        eventRepository.save(event);
    }

    private RequestStatus parseBulkStatus(String raw) {
        try {
            return RequestStatus.valueOf(raw.toUpperCase());
        } catch (Exception ex) {
            throw new ConflictException("Unknown status: " + raw);
        }
    }

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User user = getUser(userId);
        Event event = getEvent(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Cannot request participation in own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }
        if (requestRepository.findByEventAndRequester(event, user).isPresent()) {
            throw new ConflictException("Request already exists");
        }
        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        RequestStatus status = (event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration()))
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .status(status)
                .build();

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Cannot cancel someone else's request");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        User user = getUser(userId);
        return requestRepository.findAllByRequester(user).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = getEvent(eventId);
        requireInitiator(event, userId, "view requests for this event");

        return requestRepository.findAllByEvent(event).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto confirmRequest(Long userId, Long eventId, Long reqId) {
        Event event = getEvent(eventId);
        ParticipationRequest request = requestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        requireInitiator(event, userId, "confirm requests");
        if (!request.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Request does not belong to event");
        }
        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        request.setStatus(RequestStatus.CONFIRMED);
        requestRepository.save(request);

        refreshConfirmedCount(event);

        return RequestMapper.toDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto rejectRequest(Long userId, Long eventId, Long reqId) {
        Event event = getEvent(eventId);
        ParticipationRequest request = requestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        requireInitiator(event, userId, "reject requests");
        if (!request.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Request does not belong to event");
        }

        request.setStatus(RequestStatus.REJECTED);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public RequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, RequestStatusUpdateDto updateDto) {
        Event event = getEvent(eventId);
        requireInitiator(event, userId, "update requests");

        if (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0) {
            throw new ConflictException("Request moderation is disabled or no participant limit");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateDto.getRequestIds());
        if (requests.isEmpty()) {
            return RequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        RequestStatus target = parseBulkStatus(updateDto.getStatus());

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request does not belong to this event");
            }
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Only pending requests can be updated");
            }

            if (target == RequestStatus.CONFIRMED) {
                long confirmedCount = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
                if (event.getParticipantLimit() != 0 && confirmedCount >= event.getParticipantLimit()) {
                    throw new ConflictException("Cannot confirm request: participant limit reached");
                }
                req.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(RequestMapper.toDto(requestRepository.save(req)));
            } else if (target == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(requestRepository.save(req)));
            } else {
                throw new ConflictException("Unsupported bulk status: " + target);
            }
        }

        refreshConfirmedCount(event);

        return RequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}