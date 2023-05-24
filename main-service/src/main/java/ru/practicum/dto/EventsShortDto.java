package ru.practicum.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventsShortDto {
    private Long id;
    private String annotation;
    private NewCategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}
