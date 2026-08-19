package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShipmentExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ShipmentEventResponse> handleValidationException(MethodArgumentNotValidException exception) {
        ShipmentEventResponse response = handleException("Request failed validation");

        Object target = exception.getBindingResult().getTarget();
        if (target instanceof ShipmentEventRequest request) {
            response.setEventId(request.getEventId());
            response.setShipmentId(request.getShipmentId());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ShipmentEventResponse> handleUnreadableMessageException(HttpMessageNotReadableException exception) {
        ShipmentEventResponse response = handleException("Request body could not be parsed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ShipmentEventResponse handleException(String reason) {
        ShipmentEventResponse response = new ShipmentEventResponse();
        response.setReason(reason);
        response.setOutcome(IngestionOutcome.INVALID);
        return response;
    }
}



