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

    private final WarehouseActivities warehouseActivities = Workflow.newActivityStub(
        WarehouseActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    );

    @Override
    public String reserve(String sku, int quantity) {
        for (String warehouseId : WAREHOUSES) {
            log.info("Trying warehouse {}", warehouseId);
            String reservationId = warehouseActivities.checkWarehouseInventory(warehouseId, sku, quantity);
            if (reservationId != null) {
                log.info("Reserved at {}: {}", warehouseId, reservationId);
                return reservationId;
            }
        }
        throw ApplicationFailure.newNonRetryableFailure(
            "No warehouse has stock for SKU " + sku, "OutOfStock");
    }
}
