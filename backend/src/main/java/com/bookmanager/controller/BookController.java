package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.BookRequest;
import com.bookmanager.dto.PageResult;
import com.bookmanager.entity.Book;
import com.bookmanager.entity.Category;
import com.bookmanager.entity.InventoryLog;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.repository.CategoryRepository;
import com.bookmanager.repository.InventoryLogRepository;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @GetMapping("/books")
    public ApiResponse<PageResult<Book>> listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Book> bookPage = bookService.searchBooks(
                keyword, categoryId, null,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        PageResult<Book> result = PageResult.<Book>builder()
                .content(bookPage.getContent())
                .page(bookPage.getNumber())
                .size(bookPage.getSize())
                .totalElements(bookPage.getTotalElements())
                .totalPages(bookPage.getTotalPages())
                .build();
        return ApiResponse.success(result);
    }

    @GetMapping("/books/{id}")
    public ApiResponse<Book> getBook(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(book -> ApiResponse.success(book))
                .orElse(ApiResponse.error(404, "Book not found"));
    }

    @PostMapping("/books")
    public ApiResponse<Book> addBook(@Valid @RequestBody BookRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Book added", bookService.addBook(request, principal));
    }

    @PutMapping("/books/{id}")
    public ApiResponse<Book> updateBook(@PathVariable Long id,
                                         @Valid @RequestBody BookRequest request) {
        return ApiResponse.success(bookService.updateBook(id, request));
    }

    @GetMapping("/books/{id}/inventory-logs")
    public ApiResponse<List<InventoryLog>> getInventoryLogs(@PathVariable Long id) {
        return ApiResponse.success(inventoryLogRepository.findByBookIdOrderByCreatedAtDesc(id));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> listCategories() {
        return ApiResponse.success(categoryRepository.findByParentIsNullOrderBySortOrder());
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody com.bookmanager.dto.CategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId()).orElse(null);
        }
        Category category = Category.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return ApiResponse.success(categoryRepository.save(category));
    }
}
