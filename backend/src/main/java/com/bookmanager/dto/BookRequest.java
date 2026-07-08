package com.bookmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookRequest {
    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 20, message = "ISBN must be 10-20 characters")
    private String isbn;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author must not exceed 100 characters")
    private String author;

    @Size(max = 100)
    private String publisher;

    private LocalDate publishDate;
    private Long categoryId;

    @NotNull(message = "Total copies is required")
    @Positive(message = "Total copies must be positive")
    private Integer totalCopies;

    @Size(max = 100)
    private String location;

    @Size(max = 500)
    private String coverUrl;

    @Size(max = 2000)
    private String description;
}
