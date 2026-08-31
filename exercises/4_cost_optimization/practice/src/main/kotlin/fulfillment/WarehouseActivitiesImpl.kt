package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class WarehouseActivitiesImpl : WarehouseActivities {

    companion object {
        private val log = LoggerFactory.getLogger(WarehouseActivitiesImpl::class.java)

        // Simulated network latency per warehouse (milliseconds).
        // All warehouses take ~5 seconds — this makes the serial vs. parallel
        // contrast in exercises 2 and 3 obvious and painful.
        private val LATENCY = mapOf(
            "WH-CHICAGO" to 5000L,
            "WH-SANTIAGO" to 5000L,
            "WH-AMSTERDAM" to 5000L,
            "WH-NAIROBI" to 5000L,
            "WH-SINGAPORE" to 5000L,
            "WH-SYDNEY" to 5500L
        )

        // Stock only in the 5th and 6th warehouses — so serial search burns through
        // 4 full 5-second timeouts before finding anything (~20s of pain).
        // Parallel finds WH-SINGAPORE in ~5s regardless.
        private val STOCK = mapOf(
            "WH-CHICAGO" to false,
            "WH-SANTIAGO" to false,
            "WH-AMSTERDAM" to false,
            "WH-NAIROBI" to false,
            "WH-SINGAPORE" to true,
            "WH-SYDNEY" to true
        )
    }

    override fun checkWarehouseInventory(warehouseId: String, sku: String, quantity: Int): String? {
        val latency = LATENCY.getOrDefault(warehouseId, 5000L)
        log.info("Checking {} at warehouse {} (network latency: {}ms)", sku, warehouseId, latency)

        try {
            Thread.sleep(latency)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // Occasional transient failure — Temporal retries automatically
        if (Math.random() < 0.1)
            throw ApplicationFailure.newFailure("Warehouse $warehouseId API timeout", "WarehouseError")

        val inStock = STOCK.getOrDefault(warehouseId, false)
        if (!inStock) {
            log.info("Warehouse {} — out of stock for SKU {}", warehouseId, sku)
            return null
        }

        log.info("Warehouse {} — reserved {} unit(s) of {}", warehouseId, quantity, sku)
        return "RES-$warehouseId-$sku-${System.currentTimeMillis()}"
    }
}
