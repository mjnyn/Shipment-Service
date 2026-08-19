package com.accso.shipment.service;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.domain.ShipmentStatus;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.repository.ShipmentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShipmentEventServiceTests {

    @Autowired
    private ShipmentEventService service;

    @Autowired
    private ShipmentEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void firstEventIsApplied() {
        ShipmentEventResponse response = service.ingest(event(
                "evt-1",
                "ship-1",
                ShipmentStatus.LABEL_CREATED,
                "2026-03-10T12:00:00Z",
                "2026-03-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(ShipmentStatus.LABEL_CREATED, response.getCurrentStatus());
        assertEquals(1, repository.countByShipmentId(response.getShipmentId()));
    }

    @Test
    void olderEventIsStoredButStale() {
        service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-03-10T12:00:00Z",
                "2026-03-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-1",
                "ship-1",
                ShipmentStatus.LABEL_CREATED,
                "2026-01-10T12:00:00Z",
                "2026-01-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.STALE, response.getOutcome());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
        assertEquals(2, repository.countByShipmentId(response.getShipmentId()));
    }

    @Test
    void newerEventIsApplied() {
        service.ingest(event(
                "evt-1",
                "ship-1",
                ShipmentStatus.LABEL_CREATED,
                "2026-03-10T12:00:00Z",
                "2026-03-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
    }

    @Test
    void duplicateEventPayloadNotStoredAndIsNotMarkedAsMismatch() {
        service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.DUPLICATE, response.getOutcome());
        assertFalse(response.getPayloadMismatch());
        assertEquals(1, repository.countByShipmentId(response.getShipmentId()));
    }

    @Test
    void duplicateEventPayloadNotStoredButIsMarkedAsMismatch() {
        service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.DUPLICATE, response.getOutcome());
        assertTrue(response.getPayloadMismatch());
        assertEquals(1, repository.countByShipmentId(response.getShipmentId()));
    }

    @Test
    void sameOccurredAtLaterReceivedAtWins() {
        service.ingest(event(
                "evt-1",
                "ship-2",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-2",
                "ship-2",
                ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:10Z"
        ));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(2, repository.countByShipmentId(response.getShipmentId()));
        assertEquals(ShipmentStatus.DELIVERED, response.getCurrentStatus());
    }

    @Test
    void sameOccurredAtReceivedAtGreaterEventIdWins() {
        service.ingest(event(
                "evt-1",
                "ship-1",
                ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        ShipmentEventResponse response = service.ingest(event(
                "evt-2",
                "ship-1",
                ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00Z",
                "2026-04-10T12:00:05Z"
        ));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(2, repository.countByShipmentId(response.getShipmentId()));
        assertEquals(ShipmentStatus.DELIVERED, response.getCurrentStatus());
    }

    private ShipmentEventRequest event(String eventId, String shipmentId, ShipmentStatus status, String occurredAt, String receivedAt) {
        ShipmentEventRequest request = new ShipmentEventRequest();
        request.setEventId(eventId);
        request.setShipmentId(shipmentId);
        request.setStatus(status);
        request.setOccurredAt(Instant.parse(occurredAt));
        request.setReceivedAt(Instant.parse(receivedAt));
        request.setPartner("DHL");
        request.setLocation("Cape Town");

        return request;
    }
}
