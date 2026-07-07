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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public java.util.Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    public Page<Book> searchBooks(String keyword, Long categoryId, BookStatus status, Pageable pageable) {
        // Simplified search — in production use Specification or QueryDSL
        if (keyword != null && !keyword.isBlank()) {
            return bookRepository.findAll(
                    (root, query, cb) -> {
                        String pattern = "%" + keyword + "%";
                        return cb.or(
                                cb.like(root.get("title"), pattern),
                                cb.like(root.get("author"), pattern),
                                cb.like(root.get("isbn"), pattern)
                        );
                    },
                    pageable
            );
        }
        return bookRepository.findAll(pageable);
    }

    @Transactional
    public Book addBook(BookRequest request, UserPrincipal operator) {
        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw new BusinessException("ISBN already exists: " + request.getIsbn());
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("Category not found"));
        }

        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publishDate(request.getPublishDate())
                .category(category)
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
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
                .quantityChange(request.getTotalCopies())
                .totalBefore(0)
                .totalAfter(request.getTotalCopies())
                .remark("Initial入库")
                .build());

        return book;
    }

    @Transactional
    public Book updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Book not found"));

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
        }

        // Handle total copies change
        int oldTotal = book.getTotalCopies();
        int newTotal = request.getTotalCopies();
        if (newTotal != oldTotal) {
            int diff = newTotal - oldTotal;
            int newAvailable = book.getAvailableCopies() + diff;
            if (newAvailable < 0) {
                throw new BusinessException("Cannot reduce total copies below currently borrowed count");
            }
            book.setTotalCopies(newTotal);
            book.setAvailableCopies(newAvailable);

            inventoryLogRepository.save(InventoryLog.builder()
                    .book(book)
                    .operation(diff > 0 ? InventoryOperation.INBOUND : InventoryOperation.CORRECTION)
                    .quantityChange(diff)
                    .totalBefore(oldTotal)
                    .totalAfter(newTotal)
                    .remark("Manual adjustment")
                    .build());
        }

        return bookRepository.save(book);
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
            int change = -book.getAvailableCopies();
            book.setTotalCopies(book.getTotalCopies() + change);
            book.setAvailableCopies(0);
            bookRepository.save(book);
            inventoryLogRepository.save(InventoryLog.builder()
                    .book(book)
                    .operation(newStatus == BookStatus.LOST ? InventoryOperation.LOST : InventoryOperation.WITHDRAWN)
                    .quantityChange(change)
                    .totalBefore(book.getTotalCopies() - change)
                    .totalAfter(book.getTotalCopies())
                    .remark(remark)
                    .build());
        }
    }
}
