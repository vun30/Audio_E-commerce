package org.example.audio_ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Category;
import org.example.audio_ecommerce.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        List<String> defaultCategories = List.of(
                "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp", "Turntable", "Sound Card", "DJ Controller", "Combo"
        );

        for (String name : defaultCategories) {
            categoryRepository.findByName(name)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder()
                                    .name(name)
                                    .slug(name.toLowerCase().replace(" ", "-"))
                                    .description("Danh mục thiết bị: " + name)
                                    .sortOrder(0)
                                    .build()
                    ));
        }

        System.out.println("✅ Seeded default categories successfully!");
    }
}

