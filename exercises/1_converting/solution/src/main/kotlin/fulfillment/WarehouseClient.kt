package fulfillment

import io.temporal.activity.Activity
import org.slf4j.LoggerFactory

/**
 * Test double for the warehouse's reservation API.
 *
 * Real inventory services fail intermittently under load, and this one imitates
 * that: it times out on the first two attempts of any Activity execution, then
 * succeeds. That makes the retry visible and identical on every machine.
 *
 * The pretending lives here, in the stand-in for the remote service, because that
 * is where flakiness actually comes from. Nothing in this file belongs in an
 * Activity, and nothing like it belongs in production code.
 */
object WarehouseClient {

    private val log = LoggerFactory.getLogger(WarehouseClient::class.java)

    fun reserve(sku: String, quantity: Int, orderId: String): String {
        val attempt = Activity.getExecutionContext().info.attempt
        log.info("Warehouse: reserve {} x {} (attempt {})", quantity, sku, attempt)

        if (attempt < 3) {
            throw RuntimeException("Warehouse service timed out")
        }

        return "RES-$sku-$orderId"
    }
}
