package fulfillment;

import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WarehouseActivitiesImpl implements WarehouseActivities {

    private static final Logger log = LoggerFactory.getLogger(WarehouseActivitiesImpl.class);

    // Simulated network latency per warehouse (milliseconds).
    // All warehouses take ~5 seconds — this makes the serial vs. parallel
    // contrast in exercises 2 and 3 obvious and painful.
    private static final Map<String, Long> LATENCY = new HashMap<>();

    // Stock only in the 5th and 6th warehouses — so serial search burns through
    // 4 full 5-second timeouts before finding anything (~20s of pain).
    // Parallel finds WH-SINGAPORE in ~5s regardless.
    private static final Map<String, Boolean> STOCK = new HashMap<>();

    static {
        LATENCY.put("WH-CHICAGO",    5000L);
        LATENCY.put("WH-SANTIAGO",   5000L);
        LATENCY.put("WH-AMSTERDAM",  5000L);
        LATENCY.put("WH-NAIROBI",    5000L);
        LATENCY.put("WH-SINGAPORE",  5000L);
        LATENCY.put("WH-SYDNEY",     5500L);

        STOCK.put("WH-CHICAGO",    false);
        STOCK.put("WH-SANTIAGO",   false);
        STOCK.put("WH-AMSTERDAM",  false);
        STOCK.put("WH-NAIROBI",    false);
        STOCK.put("WH-SINGAPORE",  true);
        STOCK.put("WH-SYDNEY",     true);
    }

    @Override
    public String checkWarehouseInventory(String warehouseId, String sku, int quantity) {
        long latency = LATENCY.getOrDefault(warehouseId, 5000L);
        log.info("Checking {} at warehouse {} (network latency: {}ms)", sku, warehouseId, latency);

        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Occasional transient failure — Temporal retries automatically
        if (Math.random() < 0.1)
            throw ApplicationFailure.newFailure("Warehouse " + warehouseId + " API timeout", "WarehouseError");

        Boolean inStock = STOCK.getOrDefault(warehouseId, false);
        if (!inStock) {
            log.info("Warehouse {} — out of stock for SKU {}", warehouseId, sku);
            return null;
        }

        log.info("Warehouse {} — reserved {} unit(s) of {}", warehouseId, quantity, sku);
        return "RES-" + warehouseId + "-" + sku + "-" + System.currentTimeMillis();
    }
}
