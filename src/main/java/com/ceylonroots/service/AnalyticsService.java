package com.ceylonroots.service;

import com.ceylonroots.model.*;
import com.ceylonroots.model.Currency;
import com.ceylonroots.repository.FeedbackRepository;
import com.ceylonroots.repository.OrderRepository;
import com.ceylonroots.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final FeedbackRepository feedbackRepository;

    public Map<String, Object> dashboard() {
        List<Order> orders = orderRepository.findAll();
        List<Feedback> feedback = feedbackRepository.findAll();

        Map<String, Object> data = new LinkedHashMap<>();

        // revenue by month (USD, paid orders only) — last 6 months
        List<String> months = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            months.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            double total = orders.stream()
                    .filter(o -> o.getCurrency() == Currency.USD && o.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(o -> YearMonth.from(o.getCreatedAt()).equals(ym))
                    .mapToDouble(Order::getTotal).sum();
            revenue.add(total);
        }
        data.put("revenueMonths", months);
        data.put("revenueUsd", revenue);

        // orders by status
        Map<String, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), LinkedHashMap::new, Collectors.counting()));
        data.put("ordersByStatus", byStatus);

        // sales volume by grade (kg)
        Map<String, Integer> byGrade = new LinkedHashMap<>();
        for (Product p : productRepository.findAll()) {
            int qty = orders.stream()
                    .flatMap(o -> o.getItems().stream())
                    .filter(i -> i.getGrade().equals(p.getGrade()))
                    .mapToInt(OrderItem::getQty).sum();
            byGrade.put(p.getGrade(), qty);
        }
        data.put("salesByGrade", byGrade);

        // local vs international split
        long intl = orders.stream().filter(o -> o.getDestinationType() == DestinationType.INTERNATIONAL).count();
        long local = orders.stream().filter(o -> o.getDestinationType() == DestinationType.LOCAL).count();
        data.put("international", intl);
        data.put("local", local);

        // top level stats
        double revenueUsdTotal = orders.stream()
                .filter(o -> o.getCurrency() == Currency.USD && o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotal).sum();
        double revenueLkrTotal = orders.stream()
                .filter(o -> o.getCurrency() == Currency.LKR && o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotal).sum();
        long activeShipments = orders.stream()
                .filter(o -> List.of(OrderStatus.SHIPPED, OrderStatus.IN_TRANSIT, OrderStatus.CUSTOMS).contains(o.getStatus()))
                .count();
        double avgRating = feedback.isEmpty() ? 0 : feedback.stream().mapToInt(Feedback::getRating).average().orElse(0);

        data.put("revenueUsdTotal", revenueUsdTotal);
        data.put("revenueLkrTotal", revenueLkrTotal);
        data.put("activeShipments", activeShipments);
        data.put("avgRating", Math.round(avgRating * 10.0) / 10.0);

        // sentiment distribution
        Map<String, Long> sentimentDist = feedback.stream()
                .collect(Collectors.groupingBy(f -> f.getSentimentLabel().name(), LinkedHashMap::new, Collectors.counting()));
        data.put("sentimentDistribution", sentimentDist);

        // rating distribution (1-5)
        Map<Integer, Long> ratingDist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            int rating = i;
            ratingDist.put(i, feedback.stream().filter(f -> f.getRating() == rating).count());
        }
        data.put("ratingDistribution", ratingDist);

        return data;
    }
}
