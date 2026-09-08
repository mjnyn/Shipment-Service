package com.accso.shipment.entity;

import jakarta.persistence.*;

@Entity
@Table(name="shipments")
public class Shipment {

    @Id
    private String shipmentId;

    public Shipment(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    protected Shipment() {}

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
}
