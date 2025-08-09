package ru.practicum.ewm.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.RequestStatusUpdateDto;
import ru.practicum.ewm.request.dto.RequestStatusUpdateResult;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class ParticipationRequestController {

    private final ParticipationRequestService service;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Long userId,
                                                 @RequestParam @Positive Long eventId) {
        return service.create(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        return service.cancel(userId, requestId);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        return service.getUserRequests(userId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        return service.getEventRequests(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests/{reqId}/confirm")
    public ParticipationRequestDto confirmRequest(@PathVariable @Positive Long userId,
                                                  @PathVariable @Positive Long eventId,
                                                  @PathVariable("reqId") @Positive Long requestId) {
        return service.confirmRequest(userId, eventId, requestId);
    }

    @PatchMapping("/events/{eventId}/requests/{reqId}/reject")
    public ParticipationRequestDto rejectRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long eventId,
                                                 @PathVariable("reqId") @Positive Long requestId) {
        return service.rejectRequest(userId, eventId, requestId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public RequestStatusUpdateResult updateRequestsStatus(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId,
                                                          @RequestBody RequestStatusUpdateDto updateDto) {
        return service.updateRequestsStatus(userId, eventId, updateDto);
    }
}