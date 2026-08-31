package fulfillment

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/**
 * Local activities — run in the same process as the worker.
 * No task queue round-trip. No separate schedule/start/complete history events.
 * Ideal for fast, in-process steps with no external network calls.
 */
@ActivityInterface
interface LocalFulfillmentActivities {

    @ActivityMethod
    fun validateOrder(order: Order)

    @ActivityMethod
    fun fraudCheck(order: Order): String
}
