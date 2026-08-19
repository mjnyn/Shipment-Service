package com.accso.shipment.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findExistingShipmentReturnsOk() throws Exception {
        mockMvc.perform(post("/shipment-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                  "eventId": "evt-1",
                  "partner": "dhl",
                  "shipmentId": "ship-1",
                  "status": "LABEL_CREATED",
                  "occurredAt": "2026-03-10T10:00:00Z",
                  "receivedAt": "2026-03-10T10:00:05Z",
                  "location": "Amsterdam"
                }
                """));

        mockMvc.perform(post("/shipment-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                  "eventId": "evt-2",
                  "partner": "dhl",
                  "shipmentId": "ship-1",
                  "status": "IN_TRANSIT",
                  "occurredAt": "2026-03-10T12:00:00Z",
                  "receivedAt": "2026-03-10T12:00:05Z",
                  "location": "Amsterdam"
                }
                """));

        mockMvc.perform(get("/shipments/ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value("ship-1"))
                .andExpect(jsonPath("$.currentStatus").value(ShipmentStatus.IN_TRANSIT.name()))
                .andExpect(jsonPath("$.rulingEventId").value("evt-2"))
                .andExpect(jsonPath("$.eventsStored").value(2))
                .andExpect(jsonPath("$.statusOccurredAt").value("2026-03-10T12:00:00Z"));
    }

    @Test
    void findUnknownShipmentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/shipments/ship-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findShipmentHistoryReturnsOk() throws Exception {
        mockMvc.perform(post("/shipment-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                  "eventId": "evt-2",
                  "partner": "dhl",
                  "shipmentId": "ship-1",
                  "status": "IN_TRANSIT",
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
                  "status": "LABEL_CREATED",
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
                  "eventId": "evt-3",
                  "partner": "dhl",
                  "shipmentId": "ship-2",
                  "status": "IN_TRANSIT",
                  "occurredAt": "2026-03-10T13:00:00Z",
                  "receivedAt": "2026-03-10T13:00:05Z",
                  "location": "Amsterdam"
                }
                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/shipments/ship-1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-03-10T10:00:00Z"))
                .andExpect(jsonPath("$[1].eventId").value("evt-2"))
                .andExpect(jsonPath("$[1].occurredAt").value("2026-03-10T12:00:00Z"));
    }

    @Test
    void findUnknownShipmentHistoryReturnsNotFound() throws Exception {
        mockMvc.perform(get("/shipments/ship-1/events"))
                .andExpect(status().isNotFound());
    }
}
