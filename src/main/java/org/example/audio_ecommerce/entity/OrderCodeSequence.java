package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_code_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_code_seq_date", columnNames = "order_date"))
public class OrderCodeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;   // ngày (yyyy-MM-dd)

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;    // số thứ tự cuối cùng trong ngày
}
