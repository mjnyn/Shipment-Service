package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.domain.ShipmentStatus;
import com.accso.shipment.repository.ShipmentEventRepository;
import com.accso.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentEventRepository eventRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        shipmentRepository.deleteAll();
    }

    @Test
    void appliedEventReturnsCreated() throws Exception {
        mockMvc.perform(post("/shipment-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "eventId": "evt-123",
                          "partner": "dhl",
                          "shipmentId": "ship-456",
                          "status": "IN_TRANSIT",
                          "occurredAt": "2026-03-10T12:00:00Z",
                          "receivedAt": "2026-03-10T12:00:05Z",
                          "location": "Amsterdam"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-123"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.APPLIED.name()))
                .andExpect(jsonPath("$.currentStatus").value(ShipmentStatus.IN_TRANSIT.name()));
    }

    @Test
    void staleEventReturnsCreated() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-2",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "DELIVERED",
                          "occurredAt": "2026-03-10T12:00:00Z",
                          "receivedAt": "2026-03-10T12:00:05Z",
                          "location": "Amsterdam"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "HANDED_TO_CARRIER",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Amsterdam"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.STALE.name()))
                .andExpect(jsonPath("$.currentStatus").value(ShipmentStatus.DELIVERED.name()));
    }

    @Test
    void duplicateEventReturnsOk() throws Exception {
        String payload = """
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "IN_TRANSIT",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Amsterdam"
                        }
                        """;

        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.DUPLICATE.name()))
                .andExpect(jsonPath("$.currentStatus").value(ShipmentStatus.IN_TRANSIT.name()))
                .andExpect(jsonPath("$.payloadMismatch").value(false));
    }

    @Test
    void duplicateMismatchEventReturnsOk() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "IN_TRANSIT",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Amsterdam"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "IN_TRANSIT",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Rotterdam"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.DUPLICATE.name()))
                .andExpect(jsonPath("$.currentStatus").value(ShipmentStatus.IN_TRANSIT.name()))
                .andExpect(jsonPath("$.payloadMismatch").value(true));
    }

    @Test
    void missingShipmentIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "status": "IN_TRANSIT",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Rotterdam"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("shipmentId must not be blank"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.INVALID.name()))
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void blankPartnerReturnsFieldValidationReason() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventJson().replace("\"dhl\"", "\" \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.outcome").value("INVALID"))
                .andExpect(jsonPath("$.reason").value("partner must not be blank"))
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.shipmentId").value("ship-1"))
                .andExpect(jsonPath("$.currentStatus").doesNotExist());

        assertEquals(0, eventRepository.count());
        assertEquals(0, shipmentRepository.count());
    }

    @Test
    void invalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "status": "A_RANDOM_STATUS",
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Rotterdam"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("Request contains an invalid status, timestamp, or field type"))
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.INVALID.name()))
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.shipmentId").value("ship-1"))
                .andExpect(jsonPath("$.currentStatus").doesNotExist());
        assertEquals(0, eventRepository.count());
        assertEquals(0, shipmentRepository.count());
    }

    @Test
    void invalidTimestampsEchoIdentifiersAndStoreNothing() throws Exception {
        for (String timestamp : List.of("2026-03-10T10:00:00Z", "2026-03-10T10:00:05Z")) {
            mockMvc.perform(post("/shipment-events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validEventJson().replace(timestamp, "not-a-timestamp")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.outcome").value("INVALID"))
                    .andExpect(jsonPath("$.eventId").value("evt-1"))
                    .andExpect(jsonPath("$.shipmentId").value("ship-1"))
                    .andExpect(jsonPath("$.reason").value("Request contains an invalid status, timestamp, or field type"))
                    .andExpect(jsonPath("$.currentStatus").doesNotExist());
        }
        assertEquals(0, eventRepository.count());
        assertEquals(0, shipmentRepository.count());
    }

    @Test
    void oversizedStringsReturnBadRequestAndStoreNothing() throws Exception {
        for (String value : List.of("evt-1", "ship-1", "dhl", "Amsterdam")) {
            mockMvc.perform(post("/shipment-events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validEventJson().replace(value, "x".repeat(256))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.outcome").value("INVALID"))
                    .andExpect(jsonPath("$.reason").exists())
                    .andExpect(jsonPath("$.currentStatus").doesNotExist());
        }
        assertEquals(0, eventRepository.count());
        assertEquals(0, shipmentRepository.count());
    }

    @Test
    void stringsAtMaximumLengthAreAccepted() throws Exception {
        String payload = validEventJson();
        for (String value : List.of("evt-1", "ship-1", "dhl", "Amsterdam")) {
            payload = payload.replace(value, "x".repeat(255));
        }

        mockMvc.perform(post("/shipment-events").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("APPLIED"));
        assertEquals(1, eventRepository.count());
    }

    @Test
    void locationCanStillBeNull() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventJson().replace("\"Amsterdam\"", "null")))
                .andExpect(status().isCreated());
    }

    @Test
    void malformedOrNonObjectJsonReturnsInvalid() throws Exception {
        for (String payload : List.of("{\"eventId\":", "[]", "null", "42")) {
            mockMvc.perform(post("/shipment-events").contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.outcome").value("INVALID"))
                    .andExpect(jsonPath("$.reason").exists());
        }
        assertEquals(0, eventRepository.count());
        assertEquals(0, shipmentRepository.count());
    }

    private String validEventJson() {
        return """
                {
                  "eventId": "evt-1",
                  "partner": "dhl",
                  "shipmentId": "ship-1",
                  "status": "IN_TRANSIT",
                  "occurredAt": "2026-03-10T10:00:00Z",
                  "receivedAt": "2026-03-10T10:00:05Z",
                  "location": "Amsterdam"
                }
                """;
    }
}
