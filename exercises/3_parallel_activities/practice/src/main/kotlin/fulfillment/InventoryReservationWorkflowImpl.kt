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

        // TODO Part A: Fan out — for each warehouseId in WAREHOUSES, use Async.function() to
        //   call warehouseActivities::checkWarehouseInventory with (warehouseId, sku, quantity).
        //   Collect each Promise<String> into a List<Promise<String>>.
        //   Note: Async.function() starts the activity without blocking here.
        val promises: List<Promise<String>> = emptyList() // replace this — your fan-out code here

        // TODO Part B: Call Promise.allOf(promises).get() to wait for all checks to complete.

        // TODO Part C: Iterate over promises and call p.get() on each.
        //   Return the first non-null reservationId.
        //   If all are null, throw a non-retryable ApplicationFailure with type "OutOfStock".

        TODO("Part A-C: implement parallel fan-out")
    }
}
