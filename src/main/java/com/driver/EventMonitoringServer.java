package com.driver;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventMonitoringServer {

    private static final int THREAD_POOL_SIZE = 5;
    private static final int TOTAL_EVENTS = 10;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private static final AtomicBoolean highMagnitudeEventDetected = new AtomicBoolean(false);
    private static final AtomicBoolean serverRunning = new AtomicBoolean(true);

    public static void main(String[] args) {
        try {
            startServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    private static void startServer() throws InterruptedException {
        System.out.println("Event monitoring server started. Enter 'shutdown' to stop the server manually.");

        // Start thread to wait for manual shutdown
        Thread shutdownThread = new Thread(() -> {
            try {
                waitForShutdownSignal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        shutdownThread.start();

        // Simulate event processing
        for (int i = 1; i <= TOTAL_EVENTS; i++) {
            if (!serverRunning.get()) break;

            int eventId = i;
            executorService.submit(() -> processEvent(eventId));

            Thread.sleep(300); // simulate time between events
        }

        // Wait until shutdown triggered
        shutdownLatch.await();
    }

    private static void processEvent(int eventId) {
        try {
            // Simulate event processing delay
            Thread.sleep(new Random().nextInt(500) + 200);
            System.out.println("Event " + eventId + " processed.");

            // Randomly simulate high magnitude event
            int magnitude = new Random().nextInt(10) + 1;
            if (magnitude >= 9 && highMagnitudeEventDetected.compareAndSet(false, true)) {
                System.out.println("High magnitude event detected!");
                serverRunning.set(false);
                shutdownLatch.countDown();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitForShutdownSignal() throws InterruptedException {
        while (serverRunning.get()) {
            String input = getUserInput();
            if ("shutdown".equalsIgnoreCase(input.trim())) {
                serverRunning.set(false);
                System.out.println("Shutting down the server gracefully...");
                shutdownLatch.countDown();
                break;
            }
        }
    }

    private static String getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    private static void stopServer() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        System.out.println("Server terminated.");
    }
}
