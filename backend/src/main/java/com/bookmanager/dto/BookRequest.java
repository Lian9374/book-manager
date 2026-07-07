package com.bookmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookRequest {
    @NotBlank
    private String isbn;

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String publisher;
    private LocalDate publishDate;
    private Long categoryId;

    @NotNull @Positive
    private Integer totalCopies;

    private String location;
    private String coverUrl;
    private String description;
}
