package com.ceylonroots.service;

import com.ceylonroots.model.*;
import com.ceylonroots.repository.FeedbackRepository;
import com.ceylonroots.repository.OrderRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates downloadable PDF reports using OpenPDF (LGPL/MPL — free for coursework/commercial use,
 * unlike AGPL-licensed iText 5+). Each report is built as a byte[] so the controller can stream it
 * straight back as application/pdf.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;
    private final AnalyticsService analyticsService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final Color RUST = new Color(176, 86, 43);
    private static final Color INK = new Color(27, 31, 42);
    private static final Color MUTED = new Color(107, 89, 71);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, INK);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, RUST);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, INK);
    private static final Font STAT_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
    private static final Font STAT_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, INK);

    // ==========================================================================
    // ORDERS / SALES REPORT
    // ==========================================================================
    public byte[] ordersReport() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        double revenueUsd = orders.stream()
                .filter(o -> o.getCurrency() == Currency.USD && o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotal).sum();
        double revenueLkr = orders.stream()
                .filter(o -> o.getCurrency() == Currency.LKR && o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotal).sum();
        long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long active = orders.stream()
                .filter(o -> List.of(OrderStatus.SHIPPED, OrderStatus.IN_TRANSIT, OrderStatus.CUSTOMS).contains(o.getStatus()))
                .count();

        return build(document -> {
            addHeader(document, "Orders & Sales Report", orders.size() + " orders in the system");

            addStatRow(document, new String[][]{
                    {"Export revenue", String.format("$%,.2f", revenueUsd)},
                    {"Local revenue", String.format("Rs. %,.2f", revenueLkr)},
                    {"Delivered", String.valueOf(delivered)},
                    {"Active shipments", String.valueOf(active)},
            });

            document.add(sectionTitle("Order detail"));
            PdfPTable table = new PdfPTable(new float[]{2.2f, 2.2f, 1.8f, 1.6f, 1.6f, 1.4f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            addTableHeader(table, "Order", "Buyer", "Destination", "Status", "Currency", "Total", "Placed");

            for (Order o : orders) {
                addCell(table, o.getOrderCode());
                addCell(table, o.getBuyer().getName());
                addCell(table, o.getCountry());
                addCell(table, prettyStatus(o.getStatus()));
                addCell(table, o.getCurrency().name());
                addCell(table, formatMoney(o.getTotal(), o.getCurrency()));
                addCell(table, o.getCreatedAt().format(DATE_FMT));
            }
            document.add(table);
        });
    }

    // ==========================================================================
    // FEEDBACK / SENTIMENT REPORT
    // ==========================================================================
    public byte[] feedbackReport() {
        List<Feedback> feedback = feedbackRepository.findAllByOrderByCreatedAtDesc();

        double avgRating = feedback.isEmpty() ? 0 : feedback.stream().mapToInt(Feedback::getRating).average().orElse(0);
        long pos = feedback.stream().filter(f -> f.getSentimentLabel() == SentimentLabel.POSITIVE).count();
        long neu = feedback.stream().filter(f -> f.getSentimentLabel() == SentimentLabel.NEUTRAL).count();
        long neg = feedback.stream().filter(f -> f.getSentimentLabel() == SentimentLabel.NEGATIVE).count();

        return build(document -> {
            addHeader(document, "Feedback & Sentiment Report", feedback.size() + " reviews analysed");

            addStatRow(document, new String[][]{
                    {"Average rating", String.format("%.1f / 5", avgRating)},
                    {"Positive", String.valueOf(pos)},
                    {"Neutral", String.valueOf(neu)},
                    {"Negative", String.valueOf(neg)},
            });

            document.add(sectionTitle("Review detail"));
            PdfPTable table = new PdfPTable(new float[]{1.6f, 1.8f, 0.9f, 1.3f, 3.4f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            addTableHeader(table, "Order", "Buyer", "Rating", "Sentiment", "Comment");

            for (Feedback f : feedback) {
                addCell(table, f.getOrder().getOrderCode());
                addCell(table, f.getBuyer().getName());
                addCell(table, f.getRating() + "/5");
                addCell(table, f.getSentimentLabel().name());
                addCell(table, truncate(f.getComment(), 140));
            }
            document.add(table);
        });
    }

    // ==========================================================================
    // BUSINESS SUMMARY REPORT (pulls from AnalyticsService so figures always match the dashboard)
    // ==========================================================================
    @SuppressWarnings("unchecked")
    public byte[] summaryReport() {
        Map<String, Object> data = analyticsService.dashboard();

        return build(document -> {
            addHeader(document, "Business Summary Report", "Snapshot across export operations, revenue and buyer feedback");

            addStatRow(document, new String[][]{
                    {"Export revenue", String.format("$%,.2f", (Double) data.get("revenueUsdTotal"))},
                    {"Local revenue", String.format("Rs. %,.2f", (Double) data.get("revenueLkrTotal"))},
                    {"Active shipments", String.valueOf(data.get("activeShipments"))},
                    {"Avg. rating", data.get("avgRating") + " / 5"},
            });

            document.add(sectionTitle("Orders by status"));
            document.add(mapTable((Map<String, Long>) data.get("ordersByStatus"), "Status", "Orders", true));

            document.add(sectionTitle("Sales volume by grade (kg)"));
            document.add(mapTable((Map<String, Integer>) data.get("salesByGrade"), "Grade", "kg sold", false));

            document.add(sectionTitle("Local vs. international orders"));
            document.add(new Paragraph(
                    "International: " + data.get("international") + "   ·   Local: " + data.get("local"),
                    TABLE_CELL_FONT));

            document.add(sectionTitle("Sentiment distribution"));
            document.add(mapTable((Map<String, Long>) data.get("sentimentDistribution"), "Sentiment", "Reviews", true));
        });
    }

    // ==========================================================================
    // shared PDF-building helpers
    // ==========================================================================
    private byte[] build(java.util.function.Consumer<Document> body) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            body.accept(document);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
        return out.toByteArray();
    }

    private void addHeader(Document document, String title, String subtitle) throws DocumentException {
        Paragraph brand = new Paragraph("CEYLON ROOTS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, RUST));
        brand.setSpacingAfter(2);
        document.add(brand);

        Paragraph t = new Paragraph(title, TITLE_FONT);
        t.setSpacingAfter(4);
        document.add(t);

        Paragraph sub = new Paragraph(subtitle + "  ·  generated " +
                java.time.LocalDateTime.now().format(DATE_FMT), SUBTITLE_FONT);
        sub.setSpacingAfter(16);
        document.add(sub);
    }

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(18);
        p.setSpacingAfter(4);
        return p;
    }

    /** Renders a row of "stat cards" as a borderless table — mirrors the dashboard's stat cards. */
    private void addStatRow(Document document, String[][] stats) throws DocumentException {
        PdfPTable table = new PdfPTable(stats.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        for (String[] stat : stats) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(new Color(222, 203, 163));
            cell.setPadding(10);
            cell.addElement(new Paragraph(stat[0].toUpperCase(), STAT_LABEL_FONT));
            Paragraph val = new Paragraph(stat[1], STAT_VALUE_FONT);
            val.setSpacingBefore(4);
            cell.addElement(val);
            table.addCell(cell);
        }
        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(INK);
            cell.setPadding(6);
            cell.setBorderColor(INK);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, TABLE_CELL_FONT));
        cell.setPadding(5);
        cell.setBorderColor(new Color(222, 203, 163));
        table.addCell(cell);
    }

    private <T> PdfPTable mapTable(Map<String, T> map, String keyLabel, String valLabel, boolean prettyKeys) {
        PdfPTable table = new PdfPTable(new float[]{2f, 1f});
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        addTableHeader(table, keyLabel, valLabel);
        map.forEach((k, v) -> {
            addCell(table, prettyKeys ? prettify(k) : k);
            addCell(table, String.valueOf(v));
        });
        return table;
    }

    private String prettify(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p.substring(0, 1)).append(p.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private String prettyStatus(OrderStatus status) {
        return prettify(status.name());
    }

    private String formatMoney(double v, Currency currency) {
        return currency == Currency.USD ? String.format("$%,.2f", v) : String.format("Rs. %,.2f", v);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
