package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.PolicyCategoryRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemUpdateRequest;
import org.example.audio_ecommerce.dto.response.PolicyCategoryResponse;
import org.example.audio_ecommerce.dto.response.PolicyItemResponse;
import org.example.audio_ecommerce.entity.PolicyCategory;
import org.example.audio_ecommerce.entity.PolicyItem;
import org.example.audio_ecommerce.entity.Enum.PolicyCategoryType;
import org.example.audio_ecommerce.exception.ResourceNotFoundException;
import org.example.audio_ecommerce.repository.PolicyCategoryRepository;
import org.example.audio_ecommerce.repository.PolicyItemRepository;
import org.example.audio_ecommerce.service.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final PolicyCategoryRepository policyCategoryRepository;
    private final PolicyItemRepository policyItemRepository;

    // ============= POLICY CATEGORY OPERATIONS =============

    @Override
    @Transactional
    public PolicyCategoryResponse createPolicyCategory(PolicyCategoryRequest request) {
        log.info("Creating policy category: {}", request.getName());

        PolicyCategory category = PolicyCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder())
                .policyItems(new ArrayList<>())
                .build();

        if (request.getIsActive() != null) {
            category.setActive(request.getIsActive());
        }

        PolicyCategory savedCategory = policyCategoryRepository.save(category);
        log.info("Policy category created successfully with ID: {}", savedCategory.getId());

        return mapToCategoryResponse(savedCategory, 0L);
    }

    @Override
    @Transactional
    public PolicyCategoryResponse updatePolicyCategory(UUID id, PolicyCategoryRequest request) {
        log.info("Updating policy category ID: {}", id);

        PolicyCategory category = policyCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));


        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder());

        if (request.getIsActive() != null) {
            category.setActive(request.getIsActive());
        }

        PolicyCategory updatedCategory = policyCategoryRepository.save(category);
        long itemCount = policyItemRepository.countActiveByCategoryId(updatedCategory.getId());

        log.info("Policy category updated successfully: {}", id);
        return mapToCategoryResponse(updatedCategory, itemCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyCategoryResponse getPolicyCategoryById(UUID id) {
        log.info("Getting policy category by ID: {}", id);

        PolicyCategory category = policyCategoryRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        List<PolicyItemResponse> items = category.getPolicyItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        PolicyCategoryResponse response = mapToCategoryResponse(category, (long) items.size());
        response.setPolicyItems(items);

        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public List<PolicyCategoryResponse> getAllPolicyCategories() {
        log.info("Getting all policy categories");

        List<PolicyCategory> categories = policyCategoryRepository.findAllActiveOrderByDisplayOrder();

        return categories.stream()
                .map(category -> {
                    long itemCount = policyItemRepository.countActiveByCategoryId(category.getId());
                    return mapToCategoryResponse(category, itemCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePolicyCategory(UUID id) {
        log.info("Deleting policy category ID: {}", id);

        PolicyCategory category = policyCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        category.setActive(false);
        category.setDeleteAt(LocalDateTime.now());
        policyCategoryRepository.save(category);

        // Also soft delete all items
        category.getPolicyItems().forEach(item -> {
            item.setActive(false);
            item.setDeleteAt(LocalDateTime.now());
        });
        policyItemRepository.saveAll(category.getPolicyItems());

        log.info("Policy category deleted successfully: {}", id);
    }

    // ============= POLICY ITEM OPERATIONS =============

    @Override
    @Transactional
    public PolicyItemResponse createPolicyItem(PolicyItemRequest request) {
        log.info("Creating policy item for category ID: {}", request.getPolicyCategoryId());

        PolicyCategory category = policyCategoryRepository.findById(request.getPolicyCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getPolicyCategoryId()));

        PolicyItem item = PolicyItem.builder()
                .policyCategory(category)
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .imageUrls(request.getImageUrls() != null ? new ArrayList<>(request.getImageUrls()) : new ArrayList<>())
                .build();

        if (request.getIsActive() != null) {
            item.setActive(request.getIsActive());
        }

        PolicyItem savedItem = policyItemRepository.save(item);
        log.info("Policy item created successfully with ID: {}", savedItem.getId());

        return mapToItemResponse(savedItem);
    }

    @Override
    @Transactional
    public PolicyItemResponse updatePolicyItem(UUID id, PolicyItemUpdateRequest request) {
        log.info("Updating policy item ID: {}", id);

        PolicyItem item = policyItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục với ID: " + id));

        item.setTitle(request.getTitle());
        item.setContent(request.getContent());
        item.setDisplayOrder(request.getDisplayOrder());

        if (request.getImageUrls() != null) {
            item.setImageUrls(new ArrayList<>(request.getImageUrls()));
        }

        if (request.getIsActive() != null) {
            item.setActive(request.getIsActive());
        }

        PolicyItem updatedItem = policyItemRepository.save(item);
        log.info("Policy item updated successfully: {}", id);

        return mapToItemResponse(updatedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyItemResponse getPolicyItemById(UUID id) {
        log.info("Getting policy item by ID: {}", id);

        PolicyItem item = policyItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục với ID: " + id));

        return mapToItemResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyItemResponse> getPolicyItemsByCategoryId(UUID categoryId) {
        log.info("Getting policy items for category ID: {}", categoryId);

        if (!policyCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId);
        }

        List<PolicyItem> items = policyItemRepository.findActiveByCategoryIdOrderByDisplayOrder(categoryId);

        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePolicyItem(UUID id) {
        log.info("Deleting policy item ID: {}", id);

        PolicyItem item = policyItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục với ID: " + id));

        item.setActive(false);
        item.setDeleteAt(LocalDateTime.now());
        policyItemRepository.save(item);

        log.info("Policy item deleted successfully: {}", id);
    }

    // ============= MAPPER METHODS =============

    private PolicyCategoryResponse mapToCategoryResponse(PolicyCategory category, Long itemCount) {
        return PolicyCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.isActive())
                .itemCount(itemCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private PolicyItemResponse mapToItemResponse(PolicyItem item) {
        return PolicyItemResponse.builder()
                .id(item.getId())
                .policyCategoryId(item.getPolicyCategory().getId())
                .policyCategoryName(item.getPolicyCategory().getName())
                .title(item.getTitle())
                .content(item.getContent())
                .displayOrder(item.getDisplayOrder())
                .imageUrls(item.getImageUrls())
                .isActive(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}

