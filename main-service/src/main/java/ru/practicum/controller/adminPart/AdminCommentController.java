package ru.practicum.controller.adminPart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.service.CommentService;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping(path = "/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {
    private final CommentService commentService;

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long id) {
        log.info("Delete comment by admin with commentId {}", id);
        commentService.deleteCommentByAdmin(id);
    }

    @PatchMapping("{id}")
    public CommentDto updateCommentByAdmin(@PathVariable Long id,
                                           @RequestBody @Valid NewCommentDto commentDto) {
        log.info("Update comment by admin with commentId {}, commentDto {}", id, commentDto);
        return commentService.updateCommentByAdmin(id, commentDto);
    }
}
