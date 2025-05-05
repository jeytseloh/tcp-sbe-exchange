package com.exchange.matching_engine;

import com.exchange.SideEnum;

public class Order {

    public long orderId;

    public long clientId;

    public double price;

    public long quantity;

    public SideEnum side;

    public void set(long orderId, long clientId, double price, long quantity, SideEnum side) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
    }

    public void reset() {
        orderId = 0;
        clientId = 0;
        price = 0.0;
        quantity = 0;
        side = null;
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, clientId=%d, side=%s, qty=%d, price=%.2f}",
                orderId, clientId, side, quantity, price);
    }
}
