package com.accso.shipment.service;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.entity.Shipment;
import com.accso.shipment.entity.ShipmentEvent;
import com.accso.shipment.mapper.ShipmentEventMapper;
import com.accso.shipment.repository.ShipmentEventRepository;
import com.accso.shipment.repository.ShipmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ShipmentEventService {

    private final ShipmentEventRepository eventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventMapper mapper;

    public ShipmentEventService(ShipmentEventRepository eventRepository, ShipmentRepository shipmentRepository, ShipmentEventMapper mapper) {
        this.eventRepository = eventRepository;
        this.shipmentRepository = shipmentRepository;
        this.mapper = mapper;
    }

    public ShipmentEventResponse ingest(ShipmentEventRequest requestEvent) {
        if (eventRepository.doesEventExist(requestEvent.getEventId())) {
            ShipmentEvent existingEvent = eventRepository.findById(requestEvent.getEventId()).orElseThrow();
            return generateDuplicateResponse(existingEvent, requestEvent);
        }

        Shipment shipment;
        try {
            shipment = shipmentRepository.findById(requestEvent.getShipmentId())
                    .orElseGet(() -> shipmentRepository.saveAndFlush(new Shipment(requestEvent.getShipmentId())));
        } catch (DataIntegrityViolationException exception) {
            // Another request may have created this shipment first.
            shipment = shipmentRepository.findById(requestEvent.getShipmentId()).orElseThrow(() -> exception);
        }

        ShipmentEvent newEvent = mapper.toEntity(requestEvent, shipment);
        try {
            eventRepository.insertEvent(newEvent.getEventId(), newEvent.getPartner(), newEvent.getShipmentId(),
                    newEvent.getStatus().name(), newEvent.getOccurredAt(), newEvent.getReceivedAt(), newEvent.getLocation());
        } catch (DataIntegrityViolationException exception) {
            // The repository transaction has already rolled back, so this read is safe.
            ShipmentEvent event = eventRepository.findById(requestEvent.getEventId()).orElseThrow(() -> exception);
            return generateDuplicateResponse(event, requestEvent);
        }
        ShipmentEvent currentEvent = eventRepository.getRulingEvent(shipment.getShipmentId()).orElseThrow();
        IngestionOutcome outcome = Objects.equals(currentEvent.getEventId(), newEvent.getEventId()) ? IngestionOutcome.APPLIED : IngestionOutcome.STALE;

        return mapper.toResponse(newEvent.getEventId(), shipment.getShipmentId(), outcome, currentEvent);
    }

    private ShipmentEventResponse generateDuplicateResponse(ShipmentEvent existingEvent, ShipmentEventRequest eventRequest) {
        ShipmentEvent currentEvent = eventRepository.getRulingEvent(existingEvent.getShipmentId()).orElseThrow();
        ShipmentEventResponse response = mapper.toResponse(existingEvent.getEventId(), existingEvent.getShipmentId(), IngestionOutcome.DUPLICATE, currentEvent);

        response.setPayloadMismatch(payloadMismatch(existingEvent, eventRequest));
        return response;
    }

    private boolean payloadMismatch(ShipmentEvent existingEvent, ShipmentEventRequest eventRequest) {
        return !Objects.equals(existingEvent.getShipmentId(), eventRequest.getShipmentId()) ||
                !Objects.equals(existingEvent.getPartner(), eventRequest.getPartner()) ||
                !Objects.equals(existingEvent.getStatus(), eventRequest.getStatus()) ||
                !Objects.equals(existingEvent.getLocation(), eventRequest.getLocation()) ||
                !Objects.equals(existingEvent.getOccurredAt(), eventRequest.getOccurredAt()) ||
                !Objects.equals(existingEvent.getReceivedAt(), eventRequest.getReceivedAt());
    }
}
