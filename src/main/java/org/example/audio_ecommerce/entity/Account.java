package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "accounts") // thêm để tránh trùng keyword
public class Account extends BaseEntity {

    @Column(nullable = false, length = 255) // VARCHAR(255) mặc định
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false, columnDefinition = "CHAR(36)")
    private Role role;
}
