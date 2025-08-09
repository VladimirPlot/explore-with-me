package ru.practicum.ewm.request.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestStatusUpdateDto {
    private List<Long> requestIds;
    private String status;
}