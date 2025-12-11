package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateCategoryRequest;
import org.example.audio_ecommerce.dto.request.UpdateCategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CategoryResponse;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.repository.CategoryAttributeRepository;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;

    // =============================================================
    // CREATE
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> createCategory(CreateCategoryRequest req) {
        try {
            Category parent = null;

            if (req.getParentId() != null) {
                parent = categoryRepository.findById(req.getParentId())
                        .orElseThrow(() -> new RuntimeException("Parent category not found"));
            }

            Category category = Category.builder()
                    .name(req.getName())
                    .parent(parent)
                    .build();

            categoryRepository.save(category);

            // SAVE ATTRIBUTES
            if (req.getAttributes() != null) {
                for (CreateCategoryRequest.AttributeRequest a : req.getAttributes()) {
                    CategoryAttribute attr = CategoryAttribute.builder()
                            .category(category)
                            .attributeName(a.getAttributeName())
                            .attributeLabel(a.getAttributeLabel())
                            .dataType(a.getDataType())
                            .build();

                    categoryAttributeRepository.save(attr);
                }
            }

            return ResponseEntity.ok(BaseResponse.success("Category created", category.getCategoryId()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // UPDATE
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> updateCategory(UUID categoryId, UpdateCategoryRequest req) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            // Update basic
            if (req.getName() != null) category.setName(req.getName());

            // Update parent
            if (req.getParentId() != null) {
                Category parent = categoryRepository.findById(req.getParentId())
                        .orElseThrow(() -> new RuntimeException("Parent category not found"));
                category.setParent(parent);
            }

            // DELETE attributes
            if (req.getAttributesToDelete() != null) {
                for (UUID attrId : req.getAttributesToDelete()) {
                    categoryAttributeRepository.deleteById(attrId);
                }
            }

            // UPDATE attributes
            if (req.getAttributesToUpdate() != null) {
                for (UpdateCategoryRequest.AttributeToUpdate u : req.getAttributesToUpdate()) {
                    CategoryAttribute attr = categoryAttributeRepository.findById(u.getAttributeId())
                            .orElseThrow(() -> new RuntimeException("Attribute not found"));

                    attr.setAttributeName(u.getAttributeName());
                    attr.setAttributeLabel(u.getAttributeLabel());
                    attr.setDataType(u.getDataType());

                    categoryAttributeRepository.save(attr);
                }
            }

            // ADD attributes
            if (req.getAttributesToAdd() != null) {
                for (UpdateCategoryRequest.AttributeToAdd a : req.getAttributesToAdd()) {
                    CategoryAttribute newAttr = CategoryAttribute.builder()
                            .category(category)
                            .attributeName(a.getAttributeName())
                            .attributeLabel(a.getAttributeLabel())
                            .dataType(a.getDataType())
                            .build();

                    categoryAttributeRepository.save(newAttr);
                }
            }

            categoryRepository.save(category);

            return ResponseEntity.ok(BaseResponse.success("Category updated"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // DELETE
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> deleteCategory(UUID categoryId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            // Cannot delete if category has children
            if (categoryRepository.existsByParent(category)) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("Cannot delete category with children"));
            }

            // Cannot delete if product is using this category

            if (productRepository.existsByCategoriesContaining(category)) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("Category is used by products"));
            }

            categoryAttributeRepository.deleteAll(categoryAttributeRepository.findAllByCategory_CategoryId(categoryId));

            categoryRepository.delete(category);

            return ResponseEntity.ok(BaseResponse.success("Category deleted"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // GET CATEGORY BY ID
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> getCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<CategoryAttribute> attrs = categoryAttributeRepository.findAllByCategory_CategoryId(categoryId);

        CategoryResponse res = CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .attributes(
                        attrs.stream().map(a ->
                                new CategoryResponse.AttributeResponse(
                                        a.getAttributeId(),
                                        a.getAttributeName(),
                                        a.getAttributeLabel(),
                                        a.getDataType()
                                )
                        ).toList()
                )
                .build();

        return ResponseEntity.ok(BaseResponse.success("Category detail", res));
    }

    // =============================================================
    // GET CATEGORY TREE
    // =============================================================
 @Override
public ResponseEntity<BaseResponse> getCategoryTree() {

    List<Category> all = categoryRepository.findAll();

    // FIX KEY NULL: dùng ROOT thay cho null
    Map<Object, List<Category>> grouped =
            all.stream().collect(Collectors.groupingBy(
                    c -> (c.getParent() == null)
                            ? "ROOT"
                            : c.getParent().getCategoryId()
            ));

    List<Map<String, Object>> tree = buildTree("ROOT", grouped);

    return ResponseEntity.ok(BaseResponse.success("Category tree", tree));
}


    private List<Map<String, Object>> buildTree(Object parentKey, Map<Object, List<Category>> grouped) {
    List<Category> children = grouped.get(parentKey);
    if (children == null) return List.of();

    return children.stream().map(c -> {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("categoryId", c.getCategoryId());
        node.put("name", c.getName());
        node.put("children", buildTree(c.getCategoryId(), grouped));
        return node;
    }).toList();
}


}
