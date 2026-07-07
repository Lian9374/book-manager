package com.bookmanager.entity;

import com.bookmanager.enums.InventoryOperation;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryOperation operation;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "total_before", nullable = false)
    private Integer totalBefore;

    @Column(name = "total_after", nullable = false)
    private Integer totalAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(length = 500)
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
