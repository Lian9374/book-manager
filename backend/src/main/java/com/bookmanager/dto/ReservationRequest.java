package com.bookmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {
    @NotNull
    private Long bookId;
}
