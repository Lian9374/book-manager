package com.bookmanager.service;

import com.bookmanager.dto.BookRequest;
import com.bookmanager.entity.Book;
import com.bookmanager.entity.Category;
import com.bookmanager.entity.InventoryLog;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.enums.InventoryOperation;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.BookRepository;
import com.bookmanager.repository.CategoryRepository;
import com.bookmanager.repository.InventoryLogRepository;
import com.bookmanager.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Page<Book> searchBooks(String keyword, Long categoryId, BookStatus status, Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search: title, author, ISBN
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("author"), pattern),
                        cb.like(root.get("isbn"), pattern)
                ));
            }

            // Category filter (include subcategories)
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookRepository.findAll(spec, pageable);
    }

    @Transactional
    public Book addBook(BookRequest request, UserPrincipal operator) {
        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw new BusinessException("ISBN already exists: " + request.getIsbn());
        }

        // Handle duplicate with same ISBN but existing
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("Category not found"));
        }

        int totalCopies = request.getTotalCopies() != null ? request.getTotalCopies() : 1;

        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publishDate(request.getPublishDate())
                .category(category)
                .totalCopies(totalCopies)
                .availableCopies(totalCopies)
                .location(request.getLocation())
                .coverUrl(request.getCoverUrl())
                .description(request.getDescription())
                .status(BookStatus.AVAILABLE)
                .build();

        book = bookRepository.save(book);

        // Log inventory
        inventoryLogRepository.save(InventoryLog.builder()
                .book(book)
                .operation(InventoryOperation.INBOUND)
                .quantityChange(totalCopies)
                .totalBefore(0)
                .totalAfter(totalCopies)
                .remark("Initial stock entry")
                .build());

        return book;
    }

    @Transactional
    public Book updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Book not found"));

        // Check ISBN uniqueness if changed
        if (!book.getIsbn().equals(request.getIsbn())) {
            if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
                throw new BusinessException("ISBN already exists: " + request.getIsbn());
            }
        }

        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishDate(request.getPublishDate());
        book.setLocation(request.getLocation());
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("Category not found"));
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }

        // Handle total copies change
        int oldTotal = book.getTotalCopies();
        int newTotal = request.getTotalCopies() != null ? request.getTotalCopies() : oldTotal;
        if (newTotal != oldTotal) {
            int diff = newTotal - oldTotal;
            int newAvailable = book.getAvailableCopies() + diff;
            if (newAvailable < 0) {
                throw new BusinessException("Cannot reduce total copies below currently borrowed count (" +
                        (oldTotal - book.getAvailableCopies()) + " books currently borrowed)");
            }
            book.setTotalCopies(newTotal);
            book.setAvailableCopies(newAvailable);

            inventoryLogRepository.save(InventoryLog.builder()
                    .book(book)
                    .operation(diff > 0 ? InventoryOperation.INBOUND : InventoryOperation.CORRECTION)
                    .quantityChange(diff)
                    .totalBefore(oldTotal)
                    .totalAfter(newTotal)
                    .remark("Manual inventory adjustment")
                    .build());
        }

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Book not found"));

        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new BusinessException("Cannot delete — " +
                    (book.getTotalCopies() - book.getAvailableCopies()) + " copies are currently borrowed");
        }

        inventoryLogRepository.save(InventoryLog.builder()
                .book(book)
                .operation(InventoryOperation.WITHDRAWN)
                .quantityChange(-book.getTotalCopies())
                .totalBefore(book.getTotalCopies())
                .totalAfter(0)
                .remark("Book deleted from system")
                .build());

        bookRepository.delete(book);
    }

    @Transactional
    public void updateBookStatus(Long bookId, BookStatus newStatus, String remark, UserPrincipal operator) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("Book not found"));

        BookStatus oldStatus = book.getStatus();
        book.setStatus(newStatus);
        bookRepository.save(book);

        // Handle LOST or WITHDRAWN: reduce total copies
        if ((newStatus == BookStatus.LOST || newStatus == BookStatus.WITHDRAWN) && book.getAvailableCopies() > 0) {
            int before = book.getTotalCopies();
            int change = -book.getAvailableCopies();
            book.setTotalCopies(before + change);
            book.setAvailableCopies(0);
            bookRepository.save(book);
            inventoryLogRepository.save(InventoryLog.builder()
                    .book(book)
                    .operation(newStatus == BookStatus.LOST ? InventoryOperation.LOST : InventoryOperation.WITHDRAWN)
                    .quantityChange(change)
                    .totalBefore(before)
                    .totalAfter(before + change)
                    .remark(remark)
                    .build());
        }
    }
}
