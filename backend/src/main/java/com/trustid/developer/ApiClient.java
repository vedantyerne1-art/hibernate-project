package com.trustid.developer;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_clients")
public class ApiClient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clientName;

    @Column(nullable = false, unique = true)
    private String clientKey;

    @Column(nullable = false)
    private String clientSecretHash;

    @Column(nullable = false)
    private String organizationName;

    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
