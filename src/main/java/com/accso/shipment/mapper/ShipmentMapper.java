package com.accso.shipment.mapper;

import com.accso.shipment.dto.ShipmentResponse;
import com.accso.shipment.entity.ShipmentEvent;
import org.springframework.stereotype.Component;


@Component
public class ShipmentMapper {
    public ShipmentResponse toResponse(ShipmentEvent event, long eventsStored) {
        ShipmentResponse response = new ShipmentResponse();
        response.setShipmentId(event.getShipmentId());
        response.setCurrentStatus(event.getStatus());
        response.setStatusOccurredAt(event.getOccurredAt());
        response.setRulingEventId(event.getEventId());
        response.setEventsStored(eventsStored);
        return response;
    }
}
