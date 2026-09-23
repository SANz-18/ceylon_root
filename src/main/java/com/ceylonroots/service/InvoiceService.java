package com.ceylonroots.service;

import com.ceylonroots.model.Currency;
import com.ceylonroots.model.Order;
import com.ceylonroots.model.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generates a per-order invoice PDF using OpenPDF, styled to match ReportService's reports.
 */
@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final Color RUST = new Color(176, 86, 43);
    private static final Color INK = new Color(27, 31, 42);
    private static final Color MUTED = new Color(107, 89, 71);
    private static final Color LINE = new Color(222, 203, 163);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, INK);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
    private static final Font BRAND_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, RUST);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, MUTED);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, INK);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, INK);
    private static final Font TOTAL_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
    private static final Font TOTAL_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
    private static final Font GRAND_TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, RUST);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, MUTED);

    public byte[] generate(Order order) {
        Document document = new Document(PageSize.A4, 44, 44, 50, 44);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addBrandHeader(document, order);
            document.add(spacer(14));
            addBillingRow(document, order);
            document.add(spacer(20));
            addItemsTable(document, order);
            document.add(spacer(10));
            addTotals(document, order);
            document.add(spacer(28));
            addFooter(document, order);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
        return out.toByteArray();
    }

    private void addBrandHeader(Document document, Order order) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{1f, 1f});
        header.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("CEYLON ROOTS", BRAND_FONT));
        Paragraph sub = new Paragraph("Cinnamon Export & Ordering Platform", SUBTITLE_FONT);
        sub.setSpacingBefore(2);
        left.addElement(sub);
        Paragraph sub2 = new Paragraph("Matale, Sri Lanka", SUBTITLE_FONT);
        left.addElement(sub2);
        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph("INVOICE", TITLE_FONT);
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        Paragraph invNum = new Paragraph("No. INV-" + order.getOrderCode(), SUBTITLE_FONT);
        invNum.setAlignment(Element.ALIGN_RIGHT);
        invNum.setSpacingBefore(4);
        right.addElement(invNum);
        Paragraph issued = new Paragraph("Issued " + java.time.LocalDateTime.now().format(DATE_FMT), SUBTITLE_FONT);
        issued.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(issued);
        header.addCell(right);

        document.add(header);

        LineSeparator sep = new LineSeparator(0.8f, 100, LINE, Element.ALIGN_CENTER, -2);
        document.add(new Chunk(sep));
    }

    private void addBillingRow(Document document, Order order) throws DocumentException {
        PdfPTable row = new PdfPTable(new float[]{1f, 1f, 1f});
        row.setWidthPercentage(100);

        row.addCell(infoBlock("BILLED TO", new String[][]{
                {order.getBuyer().getName(), null},
                {order.getBuyer().getEmail(), null},
                {order.getCountry(), null},
        }));

        row.addCell(infoBlock("ORDER", new String[][]{
                {"Order code", order.getOrderCode()},
                {"Tracking code", order.getTrackingCode()},
                {"Placed", order.getCreatedAt().format(DATE_FMT)},
        }));

        String paidLabel = order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "—";
        row.addCell(infoBlock("PAYMENT", new String[][]{
                {"Method", order.getPaymentMethod() != null ? prettify(order.getPaymentMethod().name()) : "—"},
                {"Status", prettify(paidLabel)},
                {"Paid", order.getPaidAt() != null ? order.getPaidAt().format(DATE_FMT) : "—"},
        }));

        document.add(row);
    }

    private PdfPCell infoBlock(String heading, String[][] lines) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        Paragraph head = new Paragraph(heading, SECTION_FONT);
        head.setSpacingAfter(5);
        cell.addElement(head);
        for (String[] line : lines) {
            if (line[1] == null) {
                cell.addElement(new Paragraph(line[0] == null ? "" : line[0], VALUE_FONT));
            } else {
                Paragraph p = new Paragraph();
                p.add(new Chunk(line[0] + ": ", LABEL_FONT));
                p.add(new Chunk(line[1], VALUE_FONT));
                cell.addElement(p);
            }
        }
        return cell;
    }

    private void addItemsTable(Document document, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{3f, 1.3f, 1.6f, 1.6f});
        table.setWidthPercentage(100);

        for (String h : new String[]{"Grade", "Qty (kg)", "Price / kg", "Line total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(INK);
            cell.setPadding(7);
            cell.setBorderColor(INK);
            if (!h.equals("Grade")) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }
        table.setHeaderRows(1);

        for (OrderItem item : order.getItems()) {
            table.addCell(bodyCell(item.getGrade(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(String.valueOf(item.getQty()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(item.getPriceEach(), order.getCurrency()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(item.getPriceEach() * item.getQty(), order.getCurrency()), Element.ALIGN_RIGHT));
        }
        document.add(table);
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_CELL_FONT));
        cell.setPadding(6);
        cell.setBorderColor(LINE);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private void addTotals(Document document, Order order) throws DocumentException {
        PdfPTable wrap = new PdfPTable(new float[]{1f, 1f});
        wrap.setWidthPercentage(100);
        PdfPCell blank = new PdfPCell();
        blank.setBorder(Rectangle.NO_BORDER);
        wrap.addCell(blank);

        PdfPCell totalsCell = new PdfPCell();
        totalsCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable totals = new PdfPTable(new float[]{1f, 1f});
        totals.setWidthPercentage(100);

        addTotalRow(totals, "Subtotal", formatMoney(order.getSubtotal(), order.getCurrency()), false);

        if (order.getCouponCode() != null && order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
            addTotalRow(totals, "Coupon (" + order.getCouponCode() + ")", "−" + formatMoney(order.getDiscountAmount(), order.getCurrency()), false);
        }

        addTotalRow(totals, "Shipping", order.getShipping() != null && order.getShipping() > 0
                ? formatMoney(order.getShipping(), order.getCurrency()) : "Free", false);

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", GRAND_TOTAL_FONT));
        totalLabelCell.setBorder(Rectangle.TOP);
        totalLabelCell.setBorderColor(INK);
        totalLabelCell.setPaddingTop(8);
        totals.addCell(totalLabelCell);
        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatMoney(order.getTotal(), order.getCurrency()), GRAND_TOTAL_FONT));
        totalValueCell.setBorder(Rectangle.TOP);
        totalValueCell.setBorderColor(INK);
        totalValueCell.setPaddingTop(8);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(totalValueCell);

        totalsCell.addElement(totals);
        wrap.addCell(totalsCell);
        document.add(wrap);
    }

    private void addTotalRow(PdfPTable totals, String label, String value, boolean bold) {
        PdfPCell l = new PdfPCell(new Phrase(label, TOTAL_LABEL_FONT));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPaddingBottom(5);
        totals.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, TOTAL_VALUE_FONT));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPaddingBottom(5);
        totals.addCell(v);
    }

    private void addFooter(Document document, Order order) throws DocumentException {
        LineSeparator sep = new LineSeparator(0.6f, 100, LINE, Element.ALIGN_CENTER, -2);
        document.add(new Chunk(sep));
        Paragraph thanks = new Paragraph("Thank you for exporting Ceylon cinnamon with us.", FOOTER_FONT);
        thanks.setSpacingBefore(8);
        document.add(thanks);
        document.add(new Paragraph("This is a system-generated invoice for order " + order.getOrderCode() + ".", FOOTER_FONT));
    }

    private Chunk spacer(float height) {
        Chunk c = new Chunk(" ");
        c.setFont(FontFactory.getFont(FontFactory.HELVETICA, height / 2));
        return c;
    }

    private String prettify(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p.substring(0, 1)).append(p.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }

    private String formatMoney(double v, Currency currency) {
        return currency == Currency.USD ? String.format("$%,.2f", v) : String.format("Rs. %,.2f", v);
    }
}
