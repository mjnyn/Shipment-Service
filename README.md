# Shipment Service

Backend service for ingesting courier shipment events and querying current shipment state and event history.

## Prerequisites 
- Java 21+
- Internet access on first run to download dependencies

## Run Tests
```bash
./mvnw test
```

## Run Service
```bash
./mvnw spring-boot:run
```
The service starts at http://localhost:8080

Ingest event example:
```bash
curl -X POST http://localhost:8080/shipment-events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-123",
    "partner": "dhl",
    "shipmentId": "ship-456",
    "status": "IN_TRANSIT",
    "occurredAt": "2026-03-10T12:00:00Z",
    "receivedAt": "2026-03-10T12:00:05Z",
    "location": "Amsterdam"
  }'
```

Get shipment events example:
```bash
curl -X GET http://localhost:8080/shipments/ship-456/events
```

Get current shipment example:
```bash
curl http://localhost:8080/shipments/ship-456
```