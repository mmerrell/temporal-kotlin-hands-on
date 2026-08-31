package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.failure.ApplicationFailure
import io.temporal.workflow.Async
import io.temporal.workflow.Promise
import io.temporal.workflow.Workflow
import java.time.Duration

class InventoryReservationWorkflowImpl : InventoryReservationWorkflow {

    companion object {
        private val log = Workflow.getLogger(InventoryReservationWorkflowImpl::class.java)

        private val WAREHOUSES = listOf(
            "WH-CHICAGO", "WH-SANTIAGO", "WH-AMSTERDAM",
            "WH-NAIROBI", "WH-SINGAPORE", "WH-SYDNEY"
        )
    }

    private val warehouseActivities: WarehouseActivities = Workflow.newActivityStub(
        WarehouseActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    override fun reserve(sku: String, quantity: Int): String {
        log.info("Checking {} warehouses in parallel for SKU {}", WAREHOUSES.size, sku)

        // Fan out: fire all warehouse checks concurrently
        val promises: List<Promise<String>> = WAREHOUSES.map { warehouseId ->
            Async.function(
                warehouseActivities::checkWarehouseInventory,
                warehouseId, sku, quantity
            )
        }

        // Wait for all to complete
        Promise.allOf(promises).get()

        // Return first successful reservation
        for (p in promises) {
            val reservationId = p.get()
            if (reservationId != null) {
                log.info("Reservation found: {}", reservationId)
                return reservationId
            }
        }

        throw ApplicationFailure.newNonRetryableFailure(
            "No warehouse has stock for SKU $sku", "OutOfStock"
        )
    }
}
