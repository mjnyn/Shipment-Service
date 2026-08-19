package com.accso.shipment.repository;

import com.accso.shipment.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, String> {

    boolean existsByEventId(String eventId);

    long countByShipmentId(String shipmentId);

    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtAscReceivedAtAscEventIdAsc(String shipmentId);

    Optional<ShipmentEvent> findFirstByShipmentIdOrderByOccurredAtDescReceivedAtDescEventIdDesc(String shipmentId);
}
