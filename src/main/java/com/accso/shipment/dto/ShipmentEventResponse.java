package com.accso.shipment.dto;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.domain.ShipmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentEventResponse {
    private String eventId;
    private String shipmentId;
    private IngestionOutcome outcome;
    private ShipmentStatus currentStatus;
    private Boolean payloadMismatch;
    private String reason;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public IngestionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(IngestionOutcome outcome) {
        this.outcome = outcome;
    }

    public ShipmentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ShipmentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Boolean getPayloadMismatch() {
        return payloadMismatch;
    }

    public void setPayloadMismatch(Boolean payloadMismatch) {
        this.payloadMismatch = payloadMismatch;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
