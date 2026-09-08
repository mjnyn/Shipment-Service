package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.service.ShipmentEventService;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/shipment-events")
public class ShipmentEventController {

    private final ShipmentEventService service;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public ShipmentEventController(ShipmentEventService service, JsonMapper jsonMapper, Validator validator) {
        this.service = service;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @PostMapping
    ResponseEntity<ShipmentEventResponse> ingest(@RequestBody JsonNode payload) {
        if (!payload.isObject()) {
            return invalid(payload, "Request body must be a JSON object");
        }

        ShipmentEventRequest request;
        try {
            request = jsonMapper.treeToValue(payload, ShipmentEventRequest.class);
        } catch (JacksonException exception) {
            return invalid(payload, "Request contains an invalid status, timestamp, or field type");
        }

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            var violation = violations.iterator().next();
            return invalid(payload, violation.getPropertyPath() + " " + violation.getMessage());
        }

        ShipmentEventResponse response = service.ingest(request);
        if (response.getOutcome() == IngestionOutcome.DUPLICATE) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private ResponseEntity<ShipmentEventResponse> invalid(JsonNode payload, String reason) {
        ShipmentEventResponse response = new ShipmentEventResponse();
        response.setEventId(payload.path("eventId").asString(null));
        response.setShipmentId(payload.path("shipmentId").asString(null));
        response.setOutcome(IngestionOutcome.INVALID);
        response.setReason(reason);
        return ResponseEntity.badRequest().body(response);
    }

}
