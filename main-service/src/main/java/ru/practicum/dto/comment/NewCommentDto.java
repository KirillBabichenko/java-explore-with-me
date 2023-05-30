package ru.practicum.dto.comment;

import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Getter
@Builder
public class NewCommentDto {
    private Long id;
    @NotBlank
    @Length(max = 1000)
    private String text;
}
