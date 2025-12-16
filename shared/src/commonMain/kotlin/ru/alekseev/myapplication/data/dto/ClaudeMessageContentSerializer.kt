package ru.alekseev.myapplication.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ClaudeMessageContentSerializer : KSerializer<ClaudeMessageContent> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ClaudeMessageContent")

    override fun serialize(encoder: Encoder, value: ClaudeMessageContent) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is ClaudeMessageContent.Text -> JsonPrimitive(value.text)
            is ClaudeMessageContent.ContentBlocks -> Json.encodeToJsonElement(
                value.blocks
            )
        }
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ClaudeMessageContent {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> ClaudeMessageContent.Text(element.content)
            is JsonArray -> ClaudeMessageContent.ContentBlocks(
                Json.decodeFromJsonElement(element)
            )
            else -> throw IllegalArgumentException("Unknown content type")
        }
    }
}
