package `in`.mohammadfaizan.minecraft.models

import java.util.UUID

/**
 * Data class representing a tracking request between players
 */
data class TrackingRequest(
    val requester: UUID,
    val requesterName: String,
    val target: UUID,
    val targetName: String,
    val timestamp: Long
) 