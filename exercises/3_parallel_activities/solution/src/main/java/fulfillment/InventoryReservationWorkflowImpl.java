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

        // Fan out: fire all warehouse checks concurrently
        List<Promise<String>> promises = new ArrayList<>();
        for (String warehouseId : WAREHOUSES) {
            Promise<String> p = Async.function(
                warehouseActivities::checkWarehouseInventory,
                warehouseId, sku, quantity
            );
            promises.add(p);
        }

        // Wait for all to complete
        Promise.allOf(promises).get();

        // Return first successful reservation
        for (Promise<String> p : promises) {
            String reservationId = p.get();
            if (reservationId != null) {
                log.info("Reservation found: {}", reservationId);
                return reservationId;
            }
        }

        throw ApplicationFailure.newNonRetryableFailure(
            "No warehouse has stock for SKU " + sku, "OutOfStock");
    }
}
