package com.accso.shipment.service;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.domain.ShipmentStatus;
import com.accso.shipment.dto.ShipmentEventRequest;
import com.accso.shipment.dto.ShipmentEventResponse;
import com.accso.shipment.entity.Shipment;
import com.accso.shipment.repository.ShipmentEventRepository;
import com.accso.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ShipmentEventServiceTests {

    @Autowired
    private ShipmentEventService service;

    @Autowired
    private ShipmentService shipmentService;

    @MockitoSpyBean
    private ShipmentEventRepository eventRepository;

    @MockitoSpyBean
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        shipmentRepository.deleteAll();
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
        assertEquals(1, eventRepository.countEvents(response.getShipmentId()));
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
        assertEquals(2, eventRepository.countEvents(response.getShipmentId()));
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
        assertEquals(1, eventRepository.countEvents(response.getShipmentId()));
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
        assertEquals(1, eventRepository.countEvents(response.getShipmentId()));
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
        assertEquals(2, eventRepository.countEvents(response.getShipmentId()));
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
        assertEquals(2, eventRepository.countEvents(response.getShipmentId()));
        assertEquals(ShipmentStatus.DELIVERED, response.getCurrentStatus());
    }

    @Test
    void duplicateIsHandledWhenExistenceCheckIsOutdated() {
        ShipmentEventRequest request = event("evt-1", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05Z");
        service.ingest(request);

        doReturn(false).when(eventRepository).doesEventExist("evt-1");

        ShipmentEventResponse response = service.ingest(request);

        assertEquals(IngestionOutcome.DUPLICATE, response.getOutcome());
        assertFalse(response.getPayloadMismatch());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
        assertEquals(1, eventRepository.count());
    }

    @Test
    void originalPayloadIsPreservedWhenExistenceCheckIsOutdated() {
        ShipmentEventRequest original = event("evt-1", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05Z");
        service.ingest(original);

        doReturn(false).when(eventRepository).doesEventExist("evt-1");
        ShipmentEventRequest duplicate = event("evt-1", "ship-1", ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05Z");

        ShipmentEventResponse response = service.ingest(duplicate);

        assertEquals(IngestionOutcome.DUPLICATE, response.getOutcome());
        assertTrue(response.getPayloadMismatch());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
        assertEquals(ShipmentStatus.IN_TRANSIT, eventRepository.findById("evt-1").orElseThrow().getStatus());
        assertEquals(1, eventRepository.count());
    }

    @Test
    void shipmentCreatedByAnotherRequestIsUsedAfterSaveFails() {
        Shipment existingShipment = shipmentRepository.saveAndFlush(new Shipment("ship-1"));
        ShipmentEventRequest request = event("evt-1", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05Z");

        doReturn(Optional.empty()).doReturn(Optional.of(existingShipment))
                .when(shipmentRepository).findById("ship-1");

        doThrow(new DataIntegrityViolationException("Shipment ID already exists"))
                .when(shipmentRepository).saveAndFlush(any(Shipment.class));

        ShipmentEventResponse response = service.ingest(request);

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals("ship-1", response.getShipmentId());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
        assertEquals(1, shipmentRepository.count());
        assertEquals(1, eventRepository.countEvents("ship-1"));
        assertEquals("ship-1", eventRepository.findById("evt-1").orElseThrow().getShipmentId());
    }

    @Test
    void nanosecondTimestampsArePreservedAndIdenticalRetryMatches() {
        ShipmentEventRequest request = event("evt-1", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00.123456789Z", "2026-04-10T12:00:05.987654321Z");
        service.ingest(request);

        ShipmentEventResponse retry = service.ingest(request);

        assertEquals(IngestionOutcome.DUPLICATE, retry.getOutcome());
        assertFalse(retry.getPayloadMismatch());
        var history = shipmentService.findShipmentEvents("ship-1").orElseThrow();
        assertEquals(1, history.size());
        assertEquals(request.getOccurredAt(), history.getFirst().getOccurredAt());
        assertEquals(request.getReceivedAt(), history.getFirst().getReceivedAt());
    }

    @Test
    void laterOccurredAtNanosecondWinsOverGreaterEventId() {
        service.ingest(event("evt-z", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00.000000100Z", "2026-04-10T12:00:05Z"));

        ShipmentEventResponse response = service.ingest(event("evt-a", "ship-1", ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00.000000200Z", "2026-04-10T12:00:05Z"));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(ShipmentStatus.DELIVERED, response.getCurrentStatus());
        var history = shipmentService.findShipmentEvents("ship-1").orElseThrow();
        assertEquals("evt-z", history.getFirst().getEventId());
        assertEquals("evt-a", history.getLast().getEventId());
        assertEquals("evt-a", shipmentService.findShipment("ship-1").orElseThrow().getRulingEventId());
    }

    @Test
    void laterReceivedAtNanosecondWinsOverGreaterEventId() {
        service.ingest(event("evt-z", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05.000000100Z"));

        ShipmentEventResponse response = service.ingest(event("evt-a", "ship-1", ShipmentStatus.DELIVERED,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05.000000200Z"));

        assertEquals(IngestionOutcome.APPLIED, response.getOutcome());
        assertEquals(ShipmentStatus.DELIVERED, response.getCurrentStatus());
        assertEquals("evt-a", shipmentService.findShipment("ship-1").orElseThrow().getRulingEventId());
    }

    @Test
    void shipmentStateRemainsConsistentWhenAnotherEventArrives() {
        service.ingest(event("evt-1", "ship-1", ShipmentStatus.IN_TRANSIT,
                "2026-04-10T12:00:00Z", "2026-04-10T12:00:05Z"));
        var eventsBeforeSecondInsert = eventRepository.getEvents("ship-1");

        service.ingest(event("evt-2", "ship-1", ShipmentStatus.DELIVERED,
                "2026-04-10T13:00:00Z", "2026-04-10T13:00:05Z"));

        // Simulate a query that finished before the second insert committed.
        doReturn(eventsBeforeSecondInsert).when(eventRepository).getEvents("ship-1");

        var response = shipmentService.findShipment("ship-1").orElseThrow();

        assertEquals("evt-1", response.getRulingEventId());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getCurrentStatus());
        assertEquals(1, response.getEventsStored());
        assertEquals(2, eventRepository.countEvents("ship-1"));
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
