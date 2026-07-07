package com.bookmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequest {
    @NotNull
    private Long bookId;
}
