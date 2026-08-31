package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class FulfillmentActivitiesImpl : FulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentActivitiesImpl::class.java)
    }

    override fun reserveInventory(order: Order): String {
        // TODO Part A: Move the reserveInventory logic from FulfillmentPipeline here.
        //              Replace raw exception throws with ApplicationFailure.newFailure(message, type).
        TODO("Part A: implement reserveInventory")
    }

    override fun processPayment(order: Order): String {
        // TODO Part A: Move processPayment logic here. Same pattern as reserveInventory.
        TODO("Part A: implement processPayment")
    }

    override fun dispatchToFulfillment(order: Order, reservationId: String): String {
        // TODO Part A: Move dispatchToFulfillment logic here.
        TODO("Part A: implement dispatchToFulfillment")
    }
}
