package ru.practicum.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.model.State;

@Data
@Builder
public class ParticipationRequestDto {
    private Long id;
    private Long event;
    private String created;
    private Long requester;
    private State status;
}
