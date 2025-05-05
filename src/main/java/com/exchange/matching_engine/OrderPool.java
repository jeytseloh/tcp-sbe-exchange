package com.exchange.matching_engine;

import java.util.ArrayDeque;
import java.util.Deque;

public class OrderPool {

    private final Deque<Order> pool = new ArrayDeque<>();

    public Order acquire() {
        return (pool.pollFirst() != null) ? pool.pollFirst() : new Order();
    }

    public void release(Order order) {
        order.reset();
        pool.offerFirst(order);
    }
}
