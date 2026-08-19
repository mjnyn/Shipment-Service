package com.accso.shipment.service;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.entity.ShipmentEvent;
import com.accso.shipment.mapper.ShipmentEventMapper;
import com.accso.shipment.repository.ShipmentEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ShipmentEventService {

    private final ShipmentEventRepository repository;
    private final ShipmentEventMapper mapper;

    public ShipmentEventService(ShipmentEventRepository repository, ShipmentEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ShipmentEventResponse ingest(ShipmentEventRequest requestEvent) {
        if (repository.existsByEventId(requestEvent.getEventId())) {
            ShipmentEvent existingEvent = repository.findById(requestEvent.getEventId()).orElseThrow();
            return generateDuplicateResponse(existingEvent, requestEvent);
        }

        ShipmentEvent newEvent;
        try {
//            Forces insert so key violation is caught
            newEvent = repository.saveAndFlush(mapper.toEntity(requestEvent));
        } catch (DataIntegrityViolationException exception) {
            ShipmentEvent event = repository.findById(requestEvent.getEventId()).orElseThrow();
            return generateDuplicateResponse(event, requestEvent);
        }
        ShipmentEvent currentEvent = repository.findFirstByShipmentIdOrderByOccurredAtDescReceivedAtDescEventIdDesc(newEvent.getShipmentId()).orElseThrow();
        IngestionOutcome outcome = Objects.equals(currentEvent.getEventId(), newEvent.getEventId()) ? IngestionOutcome.APPLIED : IngestionOutcome.STALE;

        return mapper.toResponse(newEvent.getEventId(), newEvent.getShipmentId(), outcome, currentEvent);
    }

    private ShipmentEventResponse generateDuplicateResponse(ShipmentEvent existingEvent, ShipmentEventRequest eventRequest) {
        ShipmentEvent currentEvent = repository.findFirstByShipmentIdOrderByOccurredAtDescReceivedAtDescEventIdDesc(existingEvent.getShipmentId()).orElseThrow();
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
