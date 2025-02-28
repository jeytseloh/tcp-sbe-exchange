package com.exchange.order_matching_engine;

public class Order {
    public final long orderId;
    public final String symbol;
    public final short side; // 0 = Buy, 1 = Sell
    public final long quantity;
    public final long price;
    public long remainingQty;

    public Order(long orderId, String symbol, short side, long quantity, long price) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.remainingQty = quantity;
    }

    public long getRemainingQty() {
        return remainingQty;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public short getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getPrice() {
        return price;
    }
}
