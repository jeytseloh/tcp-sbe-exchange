package com.exchange.order_matching_engine;

import com.exchange.server.ServerHandler;
import io.netty.channel.ChannelHandlerContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class OrderBook {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final PriorityQueue<Order> buyOrders; // max-heap
    private final PriorityQueue<Order> sellOrders; // min-heap
    private final ReentrantLock lock = new ReentrantLock();

    public OrderBook() {
        buyOrders = new PriorityQueue<>(Comparator.comparingLong(o -> -o.price)); // highest price first
        sellOrders = new PriorityQueue<>(Comparator.comparingLong(o -> o.price)); // lowest price first
    }

    public boolean addOrder(Order order) {
        lock.lock();
        boolean isFilled;
        try {
            if (order.side == 0) { // buy order
                isFilled = matchOrder(order, sellOrders, buyOrders);
            } else { // sell order
                isFilled = matchOrder(order, buyOrders, sellOrders);
            }
        } finally {
            lock.unlock();
        }
        return isFilled;
    }

    private boolean matchOrder(Order order, PriorityQueue<Order> oppositeBook, PriorityQueue<Order> sameBook) {
        boolean isFilled = false;
        while (!oppositeBook.isEmpty()) {
            Order bestMatch = oppositeBook.peek();
            if ((order.side == 0 && order.price < bestMatch.price) || // buy order
                    (order.side == 1 && order.price > bestMatch.price)) { // sell order
                break; // no matching orders
            }
            long fillQty = Math.min(order.remainingQty, bestMatch.remainingQty);
            System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Trade Executed: " + order.symbol +
                    " price=" + bestMatch.price + " quantity=" + fillQty);

            order.remainingQty -= fillQty;
            bestMatch.remainingQty -= fillQty;
            isFilled = true;

            if (bestMatch.remainingQty == 0) oppositeBook.poll(); // remove filled order
            if (order.remainingQty == 0) {
                printOrderBook();
                return isFilled;
            } // order fully matched
        }
        sameBook.add(order); // order not fully matched -> add to order book
        printOrderBook();
        return isFilled;
    }

    public void printOrderBook() {
        System.out.println("\n==== ORDER BOOK [ " + LocalDateTime.now().format(formatter) + "] ====");
        // print sell orders (lowest price first)
        System.out.println("SELL ORDERS:");
        System.out.printf("%-10s %-10s%n", "Price", "Quantity");
        System.out.println("--------------------");
        sellOrders.stream()
                .sorted(Comparator.comparingLong(Order::getPrice))
                .forEach(order -> System.out.printf("%-10d %-10d%n", order.getPrice(), order.getRemainingQty()));
        System.out.println("---");
        // print buy orders (highest price first)
        System.out.println("BUY ORDERS:");
        System.out.printf("%-10s %-10s%n", "Price", "Quantity");
        System.out.println("--------------------");
        buyOrders.stream()
                .sorted(Comparator.comparingLong(Order::getPrice).reversed())
                .forEach(order -> System.out.printf("%-10d %-10d%n", order.getPrice(), order.getRemainingQty()));
        System.out.println("====================\n");
    }
}
