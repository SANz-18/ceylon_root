package com.ceylonroots.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refund_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id")
    private RefundRequest refundRequest;

    @Column(nullable = false)
    private String contentType; // e.g. image/png, image/jpeg

    @Lob
    @Column(name = "image_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String imageBase64;
}
