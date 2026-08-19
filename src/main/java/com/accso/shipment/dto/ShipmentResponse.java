package com.accso.shipment.dto;

import com.accso.shipment.domain.ShipmentStatus;

import java.time.Instant;

public class ShipmentResponse {

    private String shipmentId;
    private ShipmentStatus currentStatus;
    private Instant statusOccurredAt;
    private String rulingEventId;
    private long eventsStored;

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public ShipmentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ShipmentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Instant getStatusOccurredAt() {
        return statusOccurredAt;
    }

    public void setStatusOccurredAt(Instant statusOccurredAt) {
        this.statusOccurredAt = statusOccurredAt;
    }

    public String getRulingEventId() {
        return rulingEventId;
    }

    public void setRulingEventId(String rulingEventId) {
        this.rulingEventId = rulingEventId;
    }

    public long getEventsStored() {
        return eventsStored;
    }

    public void setEventsStored(long eventsStored) {
        this.eventsStored = eventsStored;
    }
}
