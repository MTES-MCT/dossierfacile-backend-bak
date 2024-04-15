package fr.dossierfacile.common.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_access_token")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OperationAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_access_type")
    private TokenOperationAccessType operationAccessType;

    @Column(name = "content")
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    @ToString.Exclude
    private ApartmentSharing apartmentSharing;

    @Column(nullable = false, length = 96, unique = true)
    private String token;

    @CreatedDate
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(nullable = false, length = 255)
    private String email;
}
