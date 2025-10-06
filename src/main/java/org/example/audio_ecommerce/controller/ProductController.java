//package org.example.audio_ecommerce.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.request.CreateProductRequest;
//import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
//import org.example.audio_ecommerce.entity.Product;
//import org.example.audio_ecommerce.service.ProductService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//public class ProductController {
//
//    private final ProductService productService;
//
//    @PostMapping
//    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest request) {
//        return ResponseEntity.ok(productService.createProduct(request));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Product>> getAllProducts() {
//        return ResponseEntity.ok(productService.getAllProducts());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
//        return ResponseEntity.ok(productService.getProductById(id));
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Product> updateProduct(
//            @PathVariable UUID id,
//            @RequestBody UpdateProductRequest request) {
//        return ResponseEntity.ok(productService.updateProduct(id, request));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
//        productService.deleteProduct(id);
//        return ResponseEntity.noContent().build();
//    }
//}
