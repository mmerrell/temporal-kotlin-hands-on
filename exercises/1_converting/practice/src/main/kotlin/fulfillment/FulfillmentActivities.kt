package fulfillment

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface FulfillmentActivities {

    @ActivityMethod
    fun reserveInventory(order: Order): String

    @ActivityMethod
    fun processPayment(order: Order): String

    @ActivityMethod
    fun dispatchToFulfillment(order: Order, reservationId: String): String
}
