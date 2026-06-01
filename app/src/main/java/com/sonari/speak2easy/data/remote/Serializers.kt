package com.sonari.speak2easy.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The backend returns some numeric stats as either JSON numbers or strings
 * (iOS handled this with a `StringOrInt` enum). These serializers coerce either form.
 */
object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeInt() }.getOrNull()
        val prim = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return null
        return prim.intOrNull ?: prim.doubleOrNull?.toInt() ?: prim.contentOrNull?.toDoubleOrNull()?.toInt()
    }
    override fun serialize(encoder: Encoder, value: Int?) = encoder.encodeInt(value ?: 0)
}

object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeDouble() }.getOrNull()
        val prim = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return null
        return prim.doubleOrNull ?: prim.contentOrNull?.toDoubleOrNull()
    }
    override fun serialize(encoder: Encoder, value: Double?) = encoder.encodeDouble(value ?: 0.0)
}
