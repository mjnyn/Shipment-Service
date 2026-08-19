package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.service.ShipmentEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipment-events")
public class ShipmentEventController {

    private final ShipmentEventService service;

    public ShipmentEventController(ShipmentEventService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ShipmentEventResponse> ingest(@Valid @RequestBody ShipmentEventRequest request) {
        ShipmentEventResponse response = service.ingest(request);
        if (response.getOutcome() == IngestionOutcome.DUPLICATE) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
