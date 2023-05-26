package ru.practicum.dto.request;

import lombok.Builder;
import lombok.Getter;
import ru.practicum.model.State;

@Getter
@Builder
public class ParticipationRequestDto {
    private Long id;
    private Long event;
    private String created;
    private Long requester;
    private State status;
}
