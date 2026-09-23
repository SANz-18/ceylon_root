package com.ceylonroots.config;

import com.ceylonroots.model.*;
import com.ceylonroots.repository.CouponRepository;
import com.ceylonroots.repository.FeedbackRepository;
import com.ceylonroots.repository.OrderRepository;
import com.ceylonroots.repository.ProductRepository;
import com.ceylonroots.repository.UserRepository;
import com.ceylonroots.service.SentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final SentimentService sentimentService;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // already seeded

        User admin = userRepository.save(User.builder().name("Admin — Ruwan Jayasuriya").email("admin@ceylonroots.lk")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).country("Sri Lanka").active(true).build());
        User staff = userRepository.save(User.builder().name("Nadeesha Perera").email("staff@ceylonroots.lk")
                .password(passwordEncoder.encode("staff123")).role(Role.STAFF).country("Sri Lanka").active(true).build());
        User buyerUk = userRepository.save(User.builder().name("James Whitfield").email("buyer@ceylonroots.lk")
                .password(passwordEncoder.encode("buyer123")).role(Role.BUYER).country("United Kingdom").active(true).build());
        User buyerLk = userRepository.save(User.builder().name("Priya Fernando").email("priya@spicebazaar.lk")
                .password(passwordEncoder.encode("buyer123")).role(Role.BUYER).country("Sri Lanka").active(true).build());
        User buyerDe = userRepository.save(User.builder().name("Hans Bauer").email("hans@wurzhandel.de")
                .password(passwordEncoder.encode("buyer123")).role(Role.BUYER).country("Germany").active(true).build());

        Product alba = productRepository.save(Product.builder().grade("Alba").name("Alba — 6mm Quills")
                .description("The palest, finest Ceylon grade. Hand-rolled to a tight 6mm diameter.")
                .priceUsd(39.5).priceLkr(11850.0).stockKg(340).build());
        Product c5 = productRepository.save(Product.builder().grade("C5 Special").name("C5 Special — 10mm Quills")
                .description("Superior colour and aroma with a slightly larger roll.")
                .priceUsd(29.0).priceLkr(8700.0).stockKg(520).build());
        Product c4 = productRepository.save(Product.builder().grade("C4").name("C4 — 12mm Quills")
                .description("Well-balanced mid grade, consistent bark thickness.")
                .priceUsd(24.0).priceLkr(7200.0).stockKg(610).build());
        Product c3 = productRepository.save(Product.builder().grade("C3").name("C3 — 16mm Quills")
                .description("Sturdier roll, strong flavour concentration.")
                .priceUsd(19.5).priceLkr(5850.0).stockKg(480).build());
        Product m5 = productRepository.save(Product.builder().grade("M5").name("M5 — Mexican Grade")
                .description("Thicker bark cut for the Latin American market.")
                .priceUsd(16.0).priceLkr(4800.0).stockKg(390).build());
        Product h1 = productRepository.save(Product.builder().grade("H1 Quillings").name("H1 — Quillings (broken bark)")
                .description("Broken bark pieces — ideal for oil extraction and grinding.")
                .priceUsd(9.5).priceLkr(2850.0).stockKg(710).build());

        couponRepository.save(Coupon.builder()
                .code("WELCOME10").description("10% off — first order welcome discount")
                .discountType(DiscountType.PERCENTAGE).percentage(10.0)
                .active(true).usedCount(0).createdAt(LocalDateTime.now()).build());

        couponRepository.save(Coupon.builder()
                .code("EXPORT50").description("Flat discount for bulk export orders")
                .discountType(DiscountType.FIXED).fixedUsd(50.0).fixedLkr(15000.0)
                .minOrderUsd(500.0).minOrderLkr(150000.0).maxUses(100)
                .active(true).usedCount(0).createdAt(LocalDateTime.now()).build());

        couponRepository.save(Coupon.builder()
                .code("SUMMER24").description("Summer promotion (expired — shown for demo purposes)")
                .discountType(DiscountType.PERCENTAGE).percentage(15.0)
                .expiresAt(LocalDateTime.now().minusDays(10))
                .active(true).usedCount(3).createdAt(LocalDateTime.now().minusDays(40)).build());

        seedOrder(buyerUk, staff, "United Kingdom", Currency.USD, DestinationType.INTERNATIONAL,
                List.of(new Line(alba, 60), new Line(c5, 40)), OrderStatus.DELIVERED, 26, PaymentMethod.CARD,
                5, "Excellent aroma and beautifully packed, arrived faster than expected. Will order again.", null);

        seedOrder(buyerDe, staff, "Germany", Currency.USD, DestinationType.INTERNATIONAL,
                List.of(new Line(c4, 150)), OrderStatus.DELIVERED, 19, PaymentMethod.PAYPAL,
                4, "Good quality bark, consistent grading. Delivery was a little delayed but support was responsive.", null);

        seedOrder(buyerLk, staff, "Sri Lanka", Currency.LKR, DestinationType.LOCAL,
                List.of(new Line(c3, 80), new Line(h1, 100)), OrderStatus.DELIVERED, 14, PaymentMethod.LANKA_QR,
                3, "Decent bark but the last batch felt slightly stale compared to before.", null);

        seedOrder(buyerUk, staff, "United Kingdom", Currency.USD, DestinationType.INTERNATIONAL,
                List.of(new Line(alba, 30)), OrderStatus.IN_TRANSIT, 6, PaymentMethod.CARD, null, null, null);

        seedOrder(buyerDe, staff, "Germany", Currency.USD, DestinationType.INTERNATIONAL,
                List.of(new Line(c5, 70), new Line(m5, 50)), OrderStatus.SHIPPED, 4, PaymentMethod.PAYPAL, null, null, null);

        seedOrder(buyerLk, staff, "Sri Lanka", Currency.LKR, DestinationType.LOCAL,
                List.of(new Line(c3, 40)), OrderStatus.PROCESSING, 1, PaymentMethod.BANK_TRANSFER, null, null, null);

        seedOrder(buyerUk, staff, "United Kingdom", Currency.USD, DestinationType.INTERNATIONAL,
                List.of(new Line(h1, 200)), OrderStatus.DELIVERED, 33, PaymentMethod.CARD,
                2, "Shipment arrived damaged and the box was broken. Support gave a partial refund.",
                "We are sorry about the damaged carton — we have since switched to reinforced export cartons for all H1 shipments.");
    }

    private record Line(Product product, int qty) {}

    private static final List<OrderStatus> STAGES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKED,
            OrderStatus.SHIPPED, OrderStatus.IN_TRANSIT, OrderStatus.CUSTOMS, OrderStatus.DELIVERED
    );

    private void seedOrder(User buyer, User staff, String country, Currency currency, DestinationType type,
                           List<Line> lines, OrderStatus status, int daysAgo, PaymentMethod method,
                           Integer ratingStars, String comment, String adminReply) {
        LocalDateTime created = LocalDateTime.now().minusDays(daysAgo);
        double subtotal = 0;

        Order order = Order.builder()
                .orderCode("ORD-" + System.nanoTime() % 100000)
                .buyer(buyer).destinationType(type).country(country).currency(currency)
                .status(status).trackingCode("CYN-" + System.nanoTime() % 1000000)
                .paymentMethod(method).paymentStatus(PaymentStatus.PAID).paidAt(created)
                .createdAt(created).assignedStaffName(staff.getName())
                .build();

        for (Line line : lines) {
            double priceEach = currency == Currency.USD ? line.product.getPriceUsd() : line.product.getPriceLkr();
            subtotal += priceEach * line.qty;
            order.getItems().add(OrderItem.builder().order(order).product(line.product)
                    .grade(line.product.getGrade()).qty(line.qty).priceEach(priceEach).build());
        }

        double shipping = type == DestinationType.INTERNATIONAL
                ? (currency == Currency.USD ? 85 : 25500)
                : (currency == Currency.USD ? 0 : 1200);
        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setTotal(subtotal + shipping);

        int stageIdx = STAGES.indexOf(status);
        order.getHistory().add(OrderStatusHistory.builder().order(order).status(OrderStatus.CONFIRMED)
                .note("Order confirmed & payment received.").updatedBy("system").updatedAt(created).build());
        for (int i = 1; i <= stageIdx; i++) {
            order.getHistory().add(OrderStatusHistory.builder().order(order).status(STAGES.get(i))
                    .note("Status updated.").updatedBy(staff.getName())
                    .updatedAt(created.plusHours((long) (i * 34))).build());
        }

        orderRepository.save(order);

        if (status == OrderStatus.DELIVERED && ratingStars != null) {
            SentimentService.SentimentResult result = sentimentService.analyze(comment);
            feedbackRepository.save(Feedback.builder()
                    .order(order).buyer(buyer).rating(ratingStars).comment(comment)
                    .sentimentLabel(result.label()).sentimentScore(result.score())
                    .matchedKeywords(String.join(",", result.matchedKeywords()))
                    .adminReply(adminReply)
                    .createdAt(created.plusDays(9))
                    .build());
        }
    }
}
