package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateCategoryRequest;
import org.example.audio_ecommerce.dto.request.UpdateCategoryRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CategoryResponse;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.entity.CategoryAttributeOption;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;
import org.example.audio_ecommerce.repository.CategoryAttributeOptionRepository;
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
    private final CategoryAttributeOptionRepository optionRepository;
    private final ProductRepository productRepository;

    // =============================================================
    // CREATE CATEGORY
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

            // CREATE ATTRIBUTES + OPTIONS
            if (req.getAttributes() != null) {
                for (CreateCategoryRequest.AttributeRequest a : req.getAttributes()) {

                    CategoryAttribute attr = CategoryAttribute.builder()
                            .category(category)
                            .attributeName(a.getAttributeName())
                            .attributeLabel(a.getAttributeLabel())
                            .dataType(a.getDataType())
                            .build();

                    categoryAttributeRepository.save(attr);

                    // nếu attribute có options
                    if ((a.getDataType() == CategoryAttributeDataType.SELECT
                            || a.getDataType() == CategoryAttributeDataType.MULTI_SELECT)
                            && a.getOptions() != null) {

                        for (String opt : a.getOptions()) {
                            optionRepository.save(
                                    CategoryAttributeOption.builder()
                                            .attribute(attr)
                                            .optionValue(opt)
                                            .build()
                            );
                        }
                    }
                }
            }

            return ResponseEntity.ok(BaseResponse.success("Category created", category.getCategoryId()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // UPDATE CATEGORY
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> updateCategory(UUID categoryId, UpdateCategoryRequest req) {
        try {

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (req.getName() != null) category.setName(req.getName());

            if (req.getParentId() != null) {
                Category parent = categoryRepository.findById(req.getParentId())
                        .orElseThrow(() -> new RuntimeException("Parent category not found"));
                category.setParent(parent);
            }

            // DELETE ATTRIBUTES (+ options)
            if (req.getAttributesToDelete() != null) {
                for (UUID attrId : req.getAttributesToDelete()) {
                    optionRepository.deleteAllByAttribute_AttributeId(attrId);
                    categoryAttributeRepository.deleteById(attrId);
                }
            }

            // UPDATE ATTRIBUTES + OPTIONS
            if (req.getAttributesToUpdate() != null) {
                for (UpdateCategoryRequest.AttributeToUpdate u : req.getAttributesToUpdate()) {

                    CategoryAttribute attr = categoryAttributeRepository.findById(u.getAttributeId())
                            .orElseThrow(() -> new RuntimeException("Attribute not found"));

                    attr.setAttributeName(u.getAttributeName());
                    attr.setAttributeLabel(u.getAttributeLabel());
                    attr.setDataType(u.getDataType());
                    categoryAttributeRepository.save(attr);

                    // DELETE OLD OPTIONS
                    optionRepository.deleteAllByAttribute_AttributeId(attr.getAttributeId());

                    // ADD NEW OPTIONS
                    if ((u.getDataType() == CategoryAttributeDataType.SELECT
                            || u.getDataType() == CategoryAttributeDataType.MULTI_SELECT)
                            && u.getOptions() != null) {

                        for (String opt : u.getOptions()) {
                            optionRepository.save(
                                    CategoryAttributeOption.builder()
                                            .attribute(attr)
                                            .optionValue(opt)
                                            .build()
                            );
                        }
                    }
                }
            }

            // ADD NEW ATTRIBUTE + OPTIONS
            if (req.getAttributesToAdd() != null) {
                for (UpdateCategoryRequest.AttributeToAdd a : req.getAttributesToAdd()) {

                    CategoryAttribute newAttr = CategoryAttribute.builder()
                            .category(category)
                            .attributeName(a.getAttributeName())
                            .attributeLabel(a.getAttributeLabel())
                            .dataType(a.getDataType())
                            .build();

                    categoryAttributeRepository.save(newAttr);

                    if ((a.getDataType() == CategoryAttributeDataType.SELECT
                            || a.getDataType() == CategoryAttributeDataType.MULTI_SELECT)
                            && a.getOptions() != null) {

                        for (String opt : a.getOptions()) {
                            optionRepository.save(
                                    CategoryAttributeOption.builder()
                                            .attribute(newAttr)
                                            .optionValue(opt)
                                            .build()
                            );
                        }
                    }
                }
            }

            categoryRepository.save(category);

            return ResponseEntity.ok(BaseResponse.success("Category updated"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // DELETE CATEGORY
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> deleteCategory(UUID categoryId) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (categoryRepository.existsByParent(category)) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("Cannot delete category with children"));
            }

            if (productRepository.existsByCategoriesContaining(category)) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("Category is used by products"));
            }

            // DELETE all attributes + options
            List<CategoryAttribute> attrs = categoryAttributeRepository.findAllByCategory_CategoryId(categoryId);
            for (CategoryAttribute attr : attrs) {
                optionRepository.deleteAllByAttribute_AttributeId(attr.getAttributeId());
            }

            categoryAttributeRepository.deleteAll(attrs);

            categoryRepository.delete(category);

            return ResponseEntity.ok(BaseResponse.success("Category deleted"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(BaseResponse.error(e.getMessage()));
        }
    }

    // =============================================================
    // GET CATEGORY BY ID (INCLUDE OPTIONS)
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> getCategory(UUID categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<CategoryAttribute> attrs = categoryAttributeRepository.findAllByCategory_CategoryId(categoryId);

        List<CategoryResponse.AttributeResponse> attrRes =
                attrs.stream().map(a ->
                        new CategoryResponse.AttributeResponse(
                                a.getAttributeId(),
                                a.getAttributeName(),
                                a.getAttributeLabel(),
                                a.getDataType(),
                                a.getOptions().stream()
                                        .map(CategoryAttributeOption::getOptionValue)
                                        .toList()
                        )
                ).toList();

        CategoryResponse res = CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .attributes(attrRes)
                .build();

        return ResponseEntity.ok(BaseResponse.success("Category detail", res));
    }

    // =============================================================
    // CATEGORY TREE
    // =============================================================
    @Override
    public ResponseEntity<BaseResponse> getCategoryTree() {

        List<Category> all = categoryRepository.findAll();

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
