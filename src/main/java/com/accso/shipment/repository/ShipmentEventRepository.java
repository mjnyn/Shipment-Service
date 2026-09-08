package com.accso.shipment.repository;

import com.accso.shipment.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, String> {
    @Transactional
    @Modifying
    @Query(value = """
        insert into shipment_events (event_id, partner, shipment_id, status, occurred_at, received_at, location)
        values (:eventId, :partner, :shipmentId, :status, :occurredAt, :receivedAt, :location)
    """, nativeQuery = true)
    void insertEvent(String eventId, String partner, String shipmentId, String status,
                     Instant occurredAt, Instant receivedAt, String location);

    @Query("""
        select count(e) > 0
        from ShipmentEvent e
        where e.eventId = :eventId
    """)
    boolean doesEventExist(String eventId);

    @Query("""
        select count(e)
        from ShipmentEvent e
        where e.shipment.shipmentId = :shipmentId
    """)
    long countEvents(String shipmentId);


    @Query("""
        select e
        from ShipmentEvent e
        where e.shipment.shipmentId = :shipmentId
        order by e.occurredAt asc, e.receivedAt asc, e.eventId asc
    """)
    List<ShipmentEvent> getEvents(String shipmentId);


    @Query(value = """
        select *
        from shipment_events e
        where e.shipment_id = :shipmentId
        order by e.occurred_at desc, e.received_at desc, e.event_id desc
        limit 1
    """,
    nativeQuery = true)
    Optional<ShipmentEvent> getRulingEvent(String shipmentId);
}
