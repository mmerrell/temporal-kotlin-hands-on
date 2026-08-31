package fulfillment

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface WarehouseActivities {
    /** Returns a reservation ID if this warehouse has stock, null otherwise. */
    @ActivityMethod
    fun checkWarehouseInventory(warehouseId: String, sku: String, quantity: Int): String?
}
