package fulfillment

import org.slf4j.LoggerFactory

/**
 * Ad-hoc order fulfillment pipeline — the "before Temporal" version.
 *
 * Problems to spot:
 *  - Manual retry loops with Thread.sleep() — not durable, lost on crash
 *  - State in local variables — if the JVM dies after payment, we have no record
 *  - No visibility into which step we're on
 *  - Double-charge risk: payment succeeded but dispatch threw, caller retries from scratch
 *  - Every failure is treated identically: a timeout worth retrying and a declined
 *    card that never will be both get five attempts and four sleeps
 */
class FulfillmentPipeline {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentPipeline::class.java)
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 2_000L
    }

    // State tracked in local variables — lost if the JVM crashes mid-execution
    private var reservationId: String? = null
    private var paymentConfirmation: String? = null
    private var trackingNumber: String? = null

    fun process(order: Order): OrderResult {
        log.info("Starting fulfillment for order {}", order.orderId)

        // Step 1: Reserve inventory — retry manually
        for (attempt in 1..MAX_RETRIES) {
            try {
                reservationId = reserveInventory(order)
                log.info("Inventory reserved: {}", reservationId)
                break
            } catch (e: Exception) {
                log.warn("Reserve attempt {} failed: {}", attempt, e.message)
                if (attempt == MAX_RETRIES)
                    throw RuntimeException("Inventory reservation failed after $MAX_RETRIES attempts", e)
                try { Thread.sleep(RETRY_DELAY_MS) } catch (ie: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }

        // Step 2: Process payment — retry manually
        for (attempt in 1..MAX_RETRIES) {
            try {
                paymentConfirmation = processPayment(order)
                log.info("Payment confirmed: {}", paymentConfirmation)
                break
            } catch (e: Exception) {
                log.warn("Payment attempt {} failed: {}", attempt, e.message)
                if (attempt == MAX_RETRIES)
                    // Reservation already succeeded — but there's no saga to roll it back
                    throw RuntimeException("Payment failed after $MAX_RETRIES attempts", e)
                try { Thread.sleep(RETRY_DELAY_MS) } catch (ie: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }

        // Step 3: Dispatch — retry manually
        for (attempt in 1..MAX_RETRIES) {
            try {
                trackingNumber = dispatchToFulfillment(order, reservationId!!)
                log.info("Dispatched, tracking: {}", trackingNumber)
                break
            } catch (e: Exception) {
                log.warn("Dispatch attempt {} failed: {}", attempt, e.message)
                if (attempt == MAX_RETRIES)
                    // Payment already charged — customer billed but order not dispatched
                    throw RuntimeException("Dispatch failed after $MAX_RETRIES attempts", e)
                try { Thread.sleep(RETRY_DELAY_MS) } catch (ie: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }

        return OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
    }

    // ── Simulated downstream calls ────────────────────────────────────────────

    private var reserveAttempts = 0

    private fun reserveInventory(order: Order): String {
        // The warehouse service is flaky under load and times out the first
        // couple of times. Calling again is the right response.
        reserveAttempts++
        if (reserveAttempts < 3) throw Exception("Warehouse service timed out")
        return "RES-${order.itemSku}-${order.orderId}"
    }

    private fun processPayment(order: Order): String {
        // A declined card is not a glitch. The retry loop above will hammer it
        // five times anyway, because it cannot tell the two kinds of failure apart.
        if (order.totalAmount > 500.0) throw Exception("Card declined: credit limit exceeded")
        return "PAY-${order.orderId}"
    }

    private fun dispatchToFulfillment(order: Order, reservationId: String): String {
        return "TRK-${order.orderId}-${reservationId.takeLast(4)}"
    }
}
