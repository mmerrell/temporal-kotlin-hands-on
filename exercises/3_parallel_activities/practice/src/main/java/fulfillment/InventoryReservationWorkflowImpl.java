package fulfillment;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryReservationWorkflowImpl implements InventoryReservationWorkflow {

    private static final Logger log = Workflow.getLogger(InventoryReservationWorkflowImpl.class);

    private static final List<String> WAREHOUSES = Arrays.asList(
        "WH-CHICAGO", "WH-SANTIAGO", "WH-AMSTERDAM",
        "WH-NAIROBI", "WH-SINGAPORE", "WH-SYDNEY"
    );

    private final WarehouseActivities warehouseActivities = Workflow.newActivityStub(
        WarehouseActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    );

    @Override
    public String reserve(String sku, int quantity) {
        log.info("Checking {} warehouses in parallel for SKU {}", WAREHOUSES.size(), sku);

        // TODO Part A: Fan out — for each warehouseId in WAREHOUSES, use Async.function() to
        //   call warehouseActivities::checkWarehouseInventory with (warehouseId, sku, quantity).
        //   Collect each Promise<String> into a List<Promise<String>>.
        //   Note: Async.function() starts the activity without blocking here.
        List<Promise<String>> promises = new ArrayList<>();
        // ... your fan-out code here

        // TODO Part B: Call Promise.allOf(promises).get() to wait for all checks to complete.

        // TODO Part C: Iterate over promises and call p.get() on each.
        //   Return the first non-null reservationId.
        //   If all are null, throw a non-retryable ApplicationFailure with type "OutOfStock".

        return null; // replace this
    }
}
