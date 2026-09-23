package com.ceylonroots.controller;

import com.ceylonroots.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/orders")
    public ResponseEntity<byte[]> orders() {
        return pdf(reportService.ordersReport(), "ceylon-roots-orders-report");
    }

    @GetMapping("/feedback")
    public ResponseEntity<byte[]> feedback() {
        return pdf(reportService.feedbackReport(), "ceylon-roots-feedback-report");
    }

    @GetMapping("/summary")
    public ResponseEntity<byte[]> summary() {
        return pdf(reportService.summaryReport(), "ceylon-roots-business-summary");
    }

    private ResponseEntity<byte[]> pdf(byte[] content, String filenamePrefix) {
        String filename = filenamePrefix + "-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
