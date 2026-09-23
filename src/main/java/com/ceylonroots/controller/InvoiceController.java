package com.ceylonroots.controller;

import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Order;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.InvoiceService;
import com.ceylonroots.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{orderId}")
    public ResponseEntity<byte[]> invoice(@PathVariable Long orderId) {
        System.out.println("Invoice request received for order: " + orderId);
        User requester = currentUserProvider.get();
        Order order = orderService.findById(orderId);

        if (requester.getRole() == Role.BUYER && !order.getBuyer().getId().equals(requester.getId())) {
            throw new ApiException("You can only view invoices for your own orders.", HttpStatus.FORBIDDEN);
        }

        byte[] pdf = invoiceService.generate(order);
        String filename = "invoice-" + order.getOrderCode() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
