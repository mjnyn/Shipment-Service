package com.accso.shipment.entity;

import com.accso.shipment.domain.ShipmentStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="shipment_events")
public class ShipmentEvent {
    @Id
    private String eventId;

    @Column(nullable = false)
    private String partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(nullable = false, secondPrecision = 9)
    private Instant occurredAt;

    @Column(nullable = false, secondPrecision = 9)
    private Instant receivedAt;

    private String location;

    protected ShipmentEvent() {
    }

    public ShipmentEvent(String eventId, String partner, Shipment shipment, ShipmentStatus status, Instant occurredAt, Instant receivedAt, String location) {
        this.eventId = eventId;
        this.partner = partner;
        this.shipment = shipment;
        this.status = status;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.location = location;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getShipmentId() { return shipment.getShipmentId(); }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipmentId(Shipment shipment) {
        this.shipment = shipment;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
