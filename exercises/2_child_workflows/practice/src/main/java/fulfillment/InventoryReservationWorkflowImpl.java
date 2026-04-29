package fulfillment;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class InventoryReservationWorkflowImpl implements InventoryReservationWorkflow {

    private static final Logger log = Workflow.getLogger(InventoryReservationWorkflowImpl.class);

    private static final List<String> WAREHOUSES = Arrays.asList(
        "WH-CHICAGO", "WH-SANTIAGO", "WH-AMSTERDAM",
        "WH-NAIROBI", "WH-SINGAPORE", "WH-SYDNEY"
    );

    // TODO Part A: Create a WarehouseActivities stub with a 10-second StartToCloseTimeout.
    private final WarehouseActivities warehouseActivities = null; // replace this

    @Override
    public String reserve(String sku, int quantity) {
        // TODO Part B: Iterate over WAREHOUSES.
        //   Call warehouseActivities.checkWarehouseInventory(warehouseId, sku, quantity).
        //   If the result is non-null, return it immediately (first warehouse with stock wins).
        //   If no warehouse has stock, throw a non-retryable ApplicationFailure with type "OutOfStock".
        return null; // replace this
    }
}
