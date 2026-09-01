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

    // TODO Part A: Create a WarehouseActivities stub with a 30-second StartToCloseTimeout.
    //   Replace TODO(...) below. Edit that one line — don't add a second
    //   `warehouseActivities` declaration.
    private val warehouseActivities: WarehouseActivities = TODO("Part A: create the WarehouseActivities stub")

    override fun reserve(sku: String, quantity: Int): String {
        // TODO Part B: Iterate over WAREHOUSES.
        //   Call warehouseActivities.checkWarehouseInventory(warehouseId, sku, quantity).
        //   If the result is non-null, return it immediately (first warehouse with stock wins).
        //   If no warehouse has stock, throw a non-retryable ApplicationFailure with type "OutOfStock".
        TODO("Part B: implement reserve")
    }
}
