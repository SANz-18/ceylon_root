package com.ceylonroots.model;

/** Ordered stages of the export/order lifecycle. Index order matters — used to compute "next stage". */
public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    IN_TRANSIT,
    CUSTOMS,
    DELIVERED,
    CANCELLED
}
