package com.accso.shipment.controller;

import com.accso.shipment.dto.ShipmentEventHistoryResponse;
import com.accso.shipment.dto.ShipmentResponse;
import com.accso.shipment.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @GetMapping("/{shipmentId}")
    ResponseEntity<ShipmentResponse> getShipment(@PathVariable String shipmentId) {
        return service.findShipment(shipmentId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{shipmentId}/events")
    ResponseEntity<List<ShipmentEventHistoryResponse>> getShipmentEvents(@PathVariable String shipmentId) {
        return service.findShipmentEvents(shipmentId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
