package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.failure.ApplicationFailure
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
        for (warehouseId in WAREHOUSES) {
            log.info("Trying warehouse {}", warehouseId)
            val reservationId = warehouseActivities.checkWarehouseInventory(warehouseId, sku, quantity)
            if (reservationId != null) {
                log.info("Reserved at {}: {}", warehouseId, reservationId)
                return reservationId
            }
        }
        throw ApplicationFailure.newNonRetryableFailure(
            "No warehouse has stock for SKU $sku", "OutOfStock"
        )
    }
}
