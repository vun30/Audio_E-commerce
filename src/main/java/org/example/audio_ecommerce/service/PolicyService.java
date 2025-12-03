package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.PolicyCategoryRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemRequest;
import org.example.audio_ecommerce.dto.request.PolicyItemUpdateRequest;
import org.example.audio_ecommerce.dto.response.PolicyCategoryResponse;
import org.example.audio_ecommerce.dto.response.PolicyItemResponse;

import java.util.List;
import java.util.UUID;

public interface PolicyService {

    // ============= POLICY CATEGORY OPERATIONS =============

    /**
     * Create a new policy category (unlimited categories allowed)
     * @param request Policy category request data
     * @return Created policy category response
     */
    PolicyCategoryResponse createPolicyCategory(PolicyCategoryRequest request);

    /**
     * Update an existing policy category
     * @param id Category ID
     * @param request Updated policy category data
     * @return Updated policy category response
     */
    PolicyCategoryResponse updatePolicyCategory(UUID id, PolicyCategoryRequest request);

    /**
     * Get policy category by ID with all items
     * @param id Category ID
     * @return Policy category response with items
     */
    PolicyCategoryResponse getPolicyCategoryById(UUID id);

    /**
     * Get all active policy categories (without items)
     * @return List of policy category responses
     */
    List<PolicyCategoryResponse> getAllPolicyCategories();

    /**
     * Soft delete a policy category and all its items
     * @param id Category ID
     */
    void deletePolicyCategory(UUID id);

    // ============= POLICY ITEM OPERATIONS =============

    /**
     * Create a new policy item
     * @param request Policy item request data
     * @return Created policy item response
     */
    PolicyItemResponse createPolicyItem(PolicyItemRequest request);

    /**
     * Update an existing policy item
     * @param id Item ID
     * @param request Updated policy item data
     * @return Updated policy item response
     */
    PolicyItemResponse updatePolicyItem(UUID id, PolicyItemUpdateRequest request);

    /**
     * Get policy item by ID
     * @param id Item ID
     * @return Policy item response
     */
    PolicyItemResponse getPolicyItemById(UUID id);

    /**
     * Get all active policy items by category ID
     * @param categoryId Category ID
     * @return List of policy item responses
     */
    List<PolicyItemResponse> getPolicyItemsByCategoryId(UUID categoryId);

    /**
     * Soft delete a policy item
     * @param id Item ID
     */
    void deletePolicyItem(UUID id);
}

