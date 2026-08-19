package com.accso.shipment.domain;

public enum ShipmentStatus {
    LABEL_CREATED,
    HANDED_TO_CARRIER,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_EXCEPTION,
    RETURNED
}
