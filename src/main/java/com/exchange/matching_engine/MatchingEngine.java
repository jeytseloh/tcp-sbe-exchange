package com.exchange.matching_engine;

import com.exchange.SideEnum;
import org.agrona.collections.Long2ObjectHashMap;

import java.util.*;

public class MatchingEngine {

    private final TreeMap<Double, List<Order>> buyOrders = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Double, List<Order>> sellOrders = new TreeMap<>();

    private final Long2ObjectHashMap<Order> orderIdToOrderMap = new Long2ObjectHashMap<>();

    public boolean addOrder(Order order) {
        if (order == null || order.side == null) {
            return false;
        }

        TreeMap<Double, List<Order>> book = (order.side == SideEnum.BUY) ? buyOrders :
                (order.side == SideEnum.SELL) ? sellOrders : null;
        if (book == null) {
            System.out.println("Order has invalid side");
            return false;
        }

        book.computeIfAbsent(order.price, p -> new ArrayList<>()).add(order);
        orderIdToOrderMap.put(order.orderId, order);
        System.out.println("Order " + order.orderId + " added to " + order.side);

        // TO-DO: matching logic

        displayCurrentOrders();
        return true;
    }

    public Order cancelOrder(long orderId) {
        Order order = orderIdToOrderMap.get(orderId);

        if (order == null) {
            System.out.println("Order not found for orderId: " + orderId);
            return order;
        }

        TreeMap<Double, List<Order>> book = (order.side == SideEnum.BUY) ? buyOrders :
                (order.side == SideEnum.SELL) ? sellOrders : null;
        if (book == null) {
            System.out.println("Order has invalid side");
            return order;
        }

        List<Order> ordersAtPrice = book.get(order.price);
        if (ordersAtPrice != null) {
            ordersAtPrice.remove(order);
            System.out.println("Order " + orderId + " removed from " + order.side);
            if (ordersAtPrice.isEmpty()) {
                book.remove(order.price);
                System.out.println("Removing price level - no remaining orders at " + order.price);
            }
        } else {
            System.out.println("No orders at current price level " + order.price);
            return order;
        }

        displayCurrentOrders();
        return order;
    }

    public boolean isOrderIdValid(long orderId) {
        return orderIdToOrderMap.containsKey(orderId);
    }

    private void displayCurrentOrders() {
        System.out.println("======= ORDER BOOK =======");

        System.out.println("--- BUY ORDERS ---");
        for (Map.Entry<Double, List<Order>> entry : buyOrders.entrySet()) {
            double price = entry.getKey();
            List<Order> orders = entry.getValue();
            for (Order order : orders) {
                System.out.println("BUY | ID: " + order.orderId + " | Price: " + price + " | Qty: " + order.quantity);
            }
        }

        System.out.println("--- SELL ORDERS ---");
        for (Map.Entry<Double, List<Order>> entry : sellOrders.entrySet()) {
            double price = entry.getKey();
            List<Order> orders = entry.getValue();
            for (Order order : orders) {
                System.out.println("SELL | ID: " + order.orderId + " | Price: " + price + " | Qty: " + order.quantity);
            }
        }

        System.out.println("==========================");
    }
}
