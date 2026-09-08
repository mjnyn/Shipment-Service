package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShipmentExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ShipmentEventResponse> handleUnreadableMessageException(HttpMessageNotReadableException exception) {
        ShipmentEventResponse response = new ShipmentEventResponse();
        response.setReason("Request body could not be parsed");
        response.setOutcome(IngestionOutcome.INVALID);
        return ResponseEntity.badRequest().body(response);
    }
}
