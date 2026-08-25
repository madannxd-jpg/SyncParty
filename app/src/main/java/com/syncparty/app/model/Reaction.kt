package com.syncparty.app.model

import java.util.UUID

data class Reaction(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val senderName: String,
    val xOffset: Float = (10..90).random().toFloat(),
    val createdAt: Long = System.currentTimeMillis()
)
