package ru.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.model.Request;

import static ru.practicum.utility.UtilityClass.formatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapperRequest {

    public static ParticipationRequestDto toRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEventId())
                .created(request.getCreated().format(formatter))
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }
}
