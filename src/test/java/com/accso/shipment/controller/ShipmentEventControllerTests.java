package com.accso.shipment.controller;

import com.accso.shipment.domain.IngestionOutcome;
import com.accso.shipment.domain.ShipmentStatus;
import com.accso.shipment.repository.ShipmentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
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
                .andExpect(jsonPath("$.reason").exists())
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.INVALID.name()))
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void invalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shipment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "eventId": "evt-1",
                          "partner": "dhl",
                          "shipmentId": "ship-1",
                          "status": "A_RANDOM_STATUS",
                          "occurredAt": "2026-03-10T10:00:00Z",
                          "receivedAt": "2026-03-10T10:00:05Z",
                          "location": "Rotterdam"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").exists())
                .andExpect(jsonPath("$.outcome").value(IngestionOutcome.INVALID.name()));
    }
}
