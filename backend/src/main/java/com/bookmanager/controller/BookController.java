package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.BookRequest;
import com.bookmanager.dto.CategoryRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    // ── Book endpoints ──

    @GetMapping("/books")
    public ApiResponse<PageResult<Book>> listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Book> bookPage = bookService.searchBooks(
                keyword, categoryId, status,
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
        return ApiResponse.success("Book added successfully", bookService.addBook(request, principal));
    }

    @PutMapping("/books/{id}")
    public ApiResponse<Book> updateBook(@PathVariable Long id,
                                         @Valid @RequestBody BookRequest request) {
        return ApiResponse.success("Book updated", bookService.updateBook(id, request));
    }

    @DeleteMapping("/books/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ApiResponse.success("Book deleted", null);
    }

    @PutMapping("/books/{id}/report-damage")
    public ApiResponse<Void> reportDamage(@PathVariable Long id,
                                           @RequestParam(defaultValue = "Book damaged") String remark,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        bookService.updateBookStatus(id, BookStatus.DAMAGED, remark, principal);
        return ApiResponse.success("Book marked as damaged", null);
    }

    @GetMapping("/books/{id}/inventory-logs")
    public ApiResponse<List<InventoryLog>> getInventoryLogs(@PathVariable Long id) {
        return ApiResponse.success(inventoryLogRepository.findByBookIdOrderByCreatedAtDesc(id));
    }

    // ── Category endpoints ──

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    public ApiResponse<List<Category>> listCategories() {
        // Load top-level categories; children are lazy-loaded inside the transaction
        List<Category> roots = categoryRepository.findByParentIsNullOrderBySortOrder();
        // Touch children to initialize lazy proxies within transaction
        roots.forEach(root -> root.getChildren().size());
        return ApiResponse.success(roots);
    }

    @GetMapping("/categories/tree")
    @Transactional(readOnly = true)
    public ApiResponse<List<Category>> categoryTree() {
        // Full recursive tree — same as above, children loaded in transaction
        List<Category> all = categoryRepository.findAll();
        // Filter to roots (no parent); children are populated via JPA relationship
        List<Category> roots = all.stream()
                .filter(c -> c.getParent() == null)
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
        // Initialize lazy children
        roots.forEach(root -> initializeChildren(root));
        return ApiResponse.success(roots);
    }

    private void initializeChildren(Category category) {
        category.getChildren().size();
        category.getChildren().forEach(this::initializeChildren);
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new com.bookmanager.exception.BusinessException("Parent category not found"));
        }
        Category category = Category.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return ApiResponse.success("Category created", categoryRepository.save(category));
    }

    @PutMapping("/categories/{id}")
    @Transactional
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new com.bookmanager.exception.BusinessException("Category not found"));

        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : category.getSortOrder());

        if (request.getParentId() != null) {
            // Prevent circular reference
            if (request.getParentId().equals(id)) {
                throw new com.bookmanager.exception.BusinessException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new com.bookmanager.exception.BusinessException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return ApiResponse.success("Category updated", categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{id}")
    @Transactional
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new com.bookmanager.exception.BusinessException("Category not found"));

        // Check if category has children
        List<Category> children = categoryRepository.findByParentIdOrderBySortOrder(id);
        if (!children.isEmpty()) {
            throw new com.bookmanager.exception.BusinessException(
                    "Cannot delete: category has " + children.size() + " subcategories. Remove them first.");
        }

        categoryRepository.delete(category);
        return ApiResponse.success("Category deleted", null);
    }
}
