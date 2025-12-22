package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductSpecUpsertRequest;
import org.example.audio_ecommerce.dto.request.SimilarProductRequest;
import org.example.audio_ecommerce.dto.request.ThongSoKyThuatDto;
import org.example.audio_ecommerce.entity.CategoryAttribute;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.ProductAttributeValue;
import org.example.audio_ecommerce.entity.Enum.CategoryAttributeDataType;
import org.example.audio_ecommerce.repository.CategoryAttributeRepository;
import org.example.audio_ecommerce.repository.CategoryRepository; // ✅ add
import org.example.audio_ecommerce.repository.ProductAttributeValueRepository;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.service.ProductSpecService;
import org.example.audio_ecommerce.util.NumberParseUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductSpecServiceImpl implements ProductSpecService {

    private final ProductRepository productRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductAttributeValueRepository pavRepository;
    private final CategoryRepository categoryRepository; // ✅ add

    private static final String DEFAULT_CATEGORY_NAME = "Loa"; // ✅ default

    private static final List<String> TECH_KEYS = List.of(
            "frequencyLowHz",
            "frequencyHighHz",
            "powerRmsW",
            "impedanceOhm",
            "sensitivityDb",
            "thdPercent",
            "crossoverFrequencyHz"
    );

    @Override
    public void upsertSpec(UUID productId, UUID categoryId, String categoryName, ProductSpecUpsertRequest req) {
        if (req == null || req.getThongSoKyThuat() == null) return;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));

        Map<String, String> rawByAttr = toRawMap(req.getThongSoKyThuat());
        if (rawByAttr.isEmpty()) return;

        List<CategoryAttribute> attrs = categoryAttributeRepository.findByCategoryIdOrNameAndNames(
                categoryId,
                categoryName,
                new ArrayList<>(rawByAttr.keySet())
        );

        Map<String, CategoryAttribute> attrMap = attrs.stream()
                .collect(Collectors.toMap(CategoryAttribute::getAttributeName, a -> a));

        List<UUID> attrIds = attrs.stream().map(CategoryAttribute::getAttributeId).toList();
        Map<UUID, ProductAttributeValue> existing = pavRepository
                .findByProductIdAndAttrIds(productId, attrIds)
                .stream()
                .collect(Collectors.toMap(v -> v.getAttribute().getAttributeId(), v -> v));

        List<ProductAttributeValue> toSave = new ArrayList<>();

        for (var e : rawByAttr.entrySet()) {
            String attrName = e.getKey();
            String raw = e.getValue();

            CategoryAttribute attr = attrMap.get(attrName);
            if (attr == null) continue;

            ProductAttributeValue pav = existing.getOrDefault(attr.getAttributeId(), new ProductAttributeValue());
            pav.setProduct(product);
            pav.setAttribute(attr);

            if (attr.getDataType() == CategoryAttributeDataType.NUMBER) {
                BigDecimal num = NumberParseUtils.extractNumber(raw);
                pav.setValue(num != null ? num.stripTrailingZeros().toPlainString() : raw);
            } else {
                pav.setValue(raw);
            }

            toSave.add(pav);
        }

        pavRepository.saveAll(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> searchSimilar(SimilarProductRequest req) {

        // ✅ categoryId không bắt buộc nữa
        if (req == null || req.getThongSoKyThuat() == null) return List.of();

        int topN = (req.getTopN() == null || req.getTopN() <= 0) ? 20 : req.getTopN();

        Map<String, BigDecimal> target = toNumberTarget(req.getThongSoKyThuat());
        if (target.isEmpty()) return List.of();

        // ✅ DEFAULT category = Loa
        UUID categoryId = req.getCategoryId();
        if (categoryId == null) {
            categoryId = categoryRepository.findIdByName(DEFAULT_CATEGORY_NAME)
                    .orElseThrow(() -> new RuntimeException("❌ Default category 'Loa' not found"));
        }

        List<ProductAttributeValue> rows =
                pavRepository.findForSimilarity(categoryId, new ArrayList<>(target.keySet()));

        Map<UUID, Map<String, BigDecimal>> productMap = new HashMap<>();
        for (ProductAttributeValue r : rows) {
            BigDecimal num = NumberParseUtils.extractNumber(r.getValue());
            if (num == null) continue;

            UUID pid = r.getProduct().getProductId();
            String attrName = r.getAttribute().getAttributeName();

            productMap.computeIfAbsent(pid, k -> new HashMap<>()).put(attrName, num);
        }

        record Scored(UUID productId, BigDecimal score) {}
        List<Scored> scored = new ArrayList<>();

        for (var entry : productMap.entrySet()) {
            UUID pid = entry.getKey();
            Map<String, BigDecimal> actual = entry.getValue();

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal count = BigDecimal.ZERO;

            for (var t : target.entrySet()) {
                BigDecimal av = actual.get(t.getKey());
                if (av == null) continue;

                BigDecimal diff = av.subtract(t.getValue()).abs();

                //  tolerance 20% (dễ match hơn)
                BigDecimal tol = t.getValue().abs().multiply(new BigDecimal("0.20"));
                if (tol.compareTo(new BigDecimal("0.000001")) < 0) tol = new BigDecimal("0.000001");

                BigDecimal fieldScore = BigDecimal.ONE.subtract(diff.divide(tol, 6, RoundingMode.HALF_UP));
                if (fieldScore.compareTo(BigDecimal.ZERO) < 0) fieldScore = BigDecimal.ZERO;

                sum = sum.add(fieldScore);
                count = count.add(BigDecimal.ONE);
            }

            if (count.compareTo(BigDecimal.ZERO) > 0) {
                scored.add(new Scored(pid, sum.divide(count, 6, RoundingMode.HALF_UP)));
            }
        }

        scored.sort((a, b) -> b.score().compareTo(a.score()));
        return scored.stream().limit(topN).map(Scored::productId).toList();
    }

    // ===== Helpers =====

    private Map<String, String> toRawMap(ThongSoKyThuatDto dto) {
        Map<String, String> m = new HashMap<>();
        if (dto == null) return m;

        if (dto.getDaiTanSo() != null) {
            put(m, "frequencyLowHz", dto.getDaiTanSo().getTanSoThap());
            put(m, "frequencyHighHz", dto.getDaiTanSo().getTanSoCao());
        }

        put(m, "powerRmsW", dto.getCongSuat());
        put(m, "impedanceOhm", dto.getTroKhang());
        put(m, "sensitivityDb", dto.getDoNhay());
        put(m, "thdPercent", dto.getDoMeoTieng());
        put(m, "crossoverFrequencyHz", dto.getTanSoCrossover());

        return m;
    }

    private Map<String, BigDecimal> toNumberTarget(ThongSoKyThuatDto dto) {
        Map<String, BigDecimal> m = new HashMap<>();
        Map<String, String> raw = toRawMap(dto);
        for (String key : TECH_KEYS) {
            BigDecimal num = NumberParseUtils.extractNumber(raw.get(key));
            if (num != null) m.put(key, num);
        }
        return m;
    }

    private void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v.trim());
    }
}
