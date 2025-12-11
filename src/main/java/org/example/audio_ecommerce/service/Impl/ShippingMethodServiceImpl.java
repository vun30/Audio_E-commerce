//package org.example.audio_ecommerce.service.Impl;
//
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.request.ShippingMethodRequest;
//import org.example.audio_ecommerce.dto.response.BaseResponse;
//import org.example.audio_ecommerce.entity.ShippingMethod;
//import org.example.audio_ecommerce.repository.ShippingMethodRepository;
//import org.example.audio_ecommerce.service.ShippingMethodService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class ShippingMethodServiceImpl implements ShippingMethodService {
//
//    private final ShippingMethodRepository repository;
//
//    @Override
//    public ResponseEntity<BaseResponse> create(ShippingMethodRequest request) {
//        if (repository.existsByName(request.getName())) {
//            return ResponseEntity.status(HttpStatus.CONFLICT)
//                    .body(BaseResponse.error("Phương thức vận chuyển đã tồn tại"));
//        }
//
//        ShippingMethod method = ShippingMethod.builder()
//                .name(request.getName())
//                .code(request.getCode())
//                .logoUrl(request.getLogoUrl())
//                .baseFee(request.getBaseFee())
//                .feePerKg(request.getFeePerKg())
//                .estimatedDeliveryDays(request.getEstimatedDeliveryDays())
//                .supportCOD(request.getSupportCOD())
//                .supportInsurance(request.getSupportInsurance())
//                .isActive(request.getIsActive())
//                .description(request.getDescription())
//                .contactPhone(request.getContactPhone())
//                .websiteUrl(request.getWebsiteUrl())
//                .build();
//
//        repository.save(method);
//        return ResponseEntity.ok(BaseResponse.success("Tạo phương thức vận chuyển thành công", method));
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse> update(UUID id, ShippingMethodRequest request) {
//        ShippingMethod method = repository.findById(id).orElse(null);
//        if (method == null)
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(BaseResponse.error("Không tìm thấy phương thức vận chuyển"));
//
//        method.setName(request.getName());
//        method.setCode(request.getCode());
//        method.setLogoUrl(request.getLogoUrl());
//        method.setBaseFee(request.getBaseFee());
//        method.setFeePerKg(request.getFeePerKg());
//        method.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());
//        method.setSupportCOD(request.getSupportCOD());
//        method.setSupportInsurance(request.getSupportInsurance());
//        method.setIsActive(request.getIsActive());
//        method.setDescription(request.getDescription());
//        method.setContactPhone(request.getContactPhone());
//        method.setWebsiteUrl(request.getWebsiteUrl());
//
//        repository.save(method);
//        return ResponseEntity.ok(BaseResponse.success("Cập nhật thành công", method));
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse> delete(UUID id) {
//        if (!repository.existsById(id))
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(BaseResponse.error("Không tìm thấy phương thức vận chuyển"));
//
//        repository.deleteById(id);
//        return ResponseEntity.ok(BaseResponse.success("Xóa thành công"));
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse> getAll() {
//        List<ShippingMethod> list = repository.findAll();
//        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách thành công", list));
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse> getById(UUID id) {
//        ShippingMethod method = repository.findById(id).orElse(null);
//        if (method == null)
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(BaseResponse.error("Không tìm thấy phương thức vận chuyển"));
//
//        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin thành công", method));
//    }
//}
