package fulfillment

// Builds a synthetic claim of a predictable size. Repeatable text rather than
// real binary data, so the repository stays small and every run is identical.
object ClaimGenerator {

    fun generate(claimId: String, orderId: String): DamageClaim = DamageClaim(
        claimId = claimId,
        orderId = orderId,
        customerId = "CUST-42",
        customerName = "  ada LOVELACE  ",
        customerEmail = "ada@example.com",
        itemSku = "SKU-ROCKET-9",
        description = "Unit arrived with a cracked housing and a bent mounting bracket.",
        inspectionReport = filler("INSPECTION", Config.INSPECTION_REPORT_KB),
        photoBundle = filler("PHOTO", Config.PHOTO_BUNDLE_KB)
    )

    private fun filler(label: String, kilobytes: Int): String {
        val target = kilobytes * 1024
        val builder = StringBuilder(target + 128)
        var line = 0
        while (builder.length < target) {
            builder.append(label).append('-').append(line++)
                .append(": surface abrasion noted, moisture reading nominal, seal intact.\n")
        }
        return builder.toString()
    }
}
