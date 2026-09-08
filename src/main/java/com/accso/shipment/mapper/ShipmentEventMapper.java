package com.accso.shipment.mapper;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventHistoryResponse;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.entity.Shipment;
import com.accso.shipment.entity.ShipmentEvent;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventMapper {

    public ShipmentEvent toEntity(ShipmentEventRequest request, Shipment shipment) {
        return new ShipmentEvent(
                request.getEventId(),
                request.getPartner(),
                shipment,
                request.getStatus(),
                request.getOccurredAt(),
                request.getReceivedAt(),
                request.getLocation()
        );
    }

    public ShipmentEventResponse toResponse(String eventId, String shipmentId, IngestionOutcome outcome, ShipmentEvent currentEvent) {
        ShipmentEventResponse response = new ShipmentEventResponse();
        response.setEventId(eventId);
        response.setShipmentId(shipmentId);
        response.setOutcome(outcome);
        response.setCurrentStatus(currentEvent.getStatus());

        return response;
    }

    public ShipmentEventHistoryResponse toHistoryResponse(ShipmentEvent event) {
        ShipmentEventHistoryResponse response = new ShipmentEventHistoryResponse();
        response.setEventId(event.getEventId());
        response.setPartner(event.getPartner());
        response.setShipmentId(event.getShipmentId());
        response.setStatus(event.getStatus());
        response.setOccurredAt(event.getOccurredAt());
        response.setReceivedAt(event.getReceivedAt());
        response.setLocation(event.getLocation());
        return response;
    }
}
