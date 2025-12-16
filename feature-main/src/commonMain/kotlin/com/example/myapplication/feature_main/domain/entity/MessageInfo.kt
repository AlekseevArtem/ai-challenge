package com.example.myapplication.feature_main.domain.entity

import kotlinx.serialization.Serializable

/**
 * Domain model for message metadata/info
 * This is used in the domain and presentation layers
 */
@Serializable
data class MessageInfo(
    val inputTokens: Int,
    val outputTokens: Int,
    val responseTimeMs: Long,
    val model: String,
    val cost: Double
)
