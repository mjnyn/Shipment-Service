package com.accso.shipment.service;

import com.accso.shipment.dto.ShipmentEventHistoryResponse;
import com.accso.shipment.dto.ShipmentResponse;
import com.accso.shipment.entity.ShipmentEvent;
import com.accso.shipment.mapper.ShipmentEventMapper;
import com.accso.shipment.mapper.ShipmentMapper;
import com.accso.shipment.repository.ShipmentEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShipmentService {

    private final ShipmentEventRepository repository;
    private final ShipmentMapper mapper;
    private final ShipmentEventMapper eventMapper;

    public ShipmentService(ShipmentEventRepository repository, ShipmentMapper mapper, ShipmentEventMapper eventMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    public Optional<ShipmentResponse> findShipment(String shipmentId) {
        List<ShipmentEvent> events = repository.getEvents(shipmentId);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toResponse(events.getLast(), events.size()));
    }

    public Optional<List<ShipmentEventHistoryResponse>> findShipmentEvents(String shipmentId) {
        List<ShipmentEventHistoryResponse> events = repository.getEvents(shipmentId).stream().map(eventMapper::toHistoryResponse).toList();

        if (events.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(events);
    }
}
