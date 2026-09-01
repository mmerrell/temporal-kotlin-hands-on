package fulfillment

import java.util.concurrent.ConcurrentHashMap

// Stands in for the document store the claims system already has — S3, a CMS,
// a filesystem, a document database. The only thing that matters for this
// exercise is that it lives OUTSIDE Temporal, and that Temporal carries a
// reference to a document rather than the document itself.
//
// A real implementation would be a couple of SDK calls. This is an in-process
// map so the exercise needs no infrastructure. It is not durable and not shared
// between Workers — do not copy this part into anything real.
object EvidenceStore {

    private val documents = ConcurrentHashMap<String, DamageClaim>()

    fun put(claim: DamageClaim): String {
        val ref = "evidence/${claim.claimId}"
        documents[ref] = claim
        return ref
    }

    fun get(ref: String): DamageClaim =
        documents[ref] ?: error("No evidence document found at $ref")
}
