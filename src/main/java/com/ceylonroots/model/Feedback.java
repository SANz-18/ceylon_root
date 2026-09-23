package com.ceylonroots.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    private SentimentLabel sentimentLabel;

    private Integer sentimentScore;

    @Column(length = 1000)
    private String matchedKeywords; // comma-separated, for transparency in the UI

    @Column(length = 1000)
    private String adminReply;

    private LocalDateTime createdAt;
}
