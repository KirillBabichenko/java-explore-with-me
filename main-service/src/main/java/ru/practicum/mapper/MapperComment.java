package ru.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Event;
import ru.practicum.model.EventComment;
import ru.practicum.model.User;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapperComment {

    public static CommentDto toCommentDto(EventComment eventComment) {
        return CommentDto.builder()
                .id(eventComment.getId())
                .authorName(eventComment.getAuthor().getName())
                .text(eventComment.getText())
                .created(eventComment.getCreated())
                .build();
    }

    public static EventComment toComment(NewCommentDto commentDto, Event event, User user) {
        return EventComment.builder()
                .event(event)
                .author(user)
                .text(commentDto.getText())
                .created(LocalDateTime.now())
                .build();
    }
}