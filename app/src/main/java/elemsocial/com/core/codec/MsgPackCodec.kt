package elemsocial.com.core.codec

import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.value.Value

object MsgPackCodec {
    fun encodeMap(map: Map<String, Any?>): ByteArray {
        val packer = MessagePack.newDefaultBufferPacker()
        packMap(map, packer)
        packer.close()
        return packer.toByteArray()
    }

    fun decodeToAny(bytes: ByteArray): Any? {
        val unpacker = MessagePack.newDefaultUnpacker(bytes)
        val value = unpacker.unpackValue()
        unpacker.close()
        return value.toKotlinValue()
    }

    fun decodeToMap(bytes: ByteArray): Map<String, Any?> {
        val decoded = decodeToAny(bytes)
        @Suppress("UNCHECKED_CAST")
        return decoded as? Map<String, Any?> ?: emptyMap()
    }

    private fun packMap(map: Map<String, Any?>, packer: MessagePacker) {
        packer.packMapHeader(map.size)
        map.forEach { (key, value) ->
            packer.packString(key)
            packAny(value, packer)
        }
    }

    private fun packArray(list: List<*>, packer: MessagePacker) {
        packer.packArrayHeader(list.size)
        list.forEach { packAny(it, packer) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun packAny(value: Any?, packer: MessagePacker) {
        when (value) {
            null -> packer.packNil()
            is String -> packer.packString(value)
            is Int -> packer.packInt(value)
            is Long -> packer.packLong(value)
            is Short -> packer.packShort(value)
            is Byte -> packer.packByte(value)
            is Boolean -> packer.packBoolean(value)
            is Float -> packer.packFloat(value)
            is Double -> packer.packDouble(value)
            is ByteArray -> {
                packer.packBinaryHeader(value.size)
                packer.writePayload(value)
            }
            is List<*> -> packArray(value, packer)
            is Map<*, *> -> {
                val typed = value.entries.associate { (k, v) -> k.toString() to v }
                packMap(typed, packer)
            }
            else -> packer.packString(value.toString())
        }
    }

    private fun Value.toKotlinValue(): Any? {
        return when {
            isNilValue -> null
            isBooleanValue -> asBooleanValue().boolean
            isIntegerValue -> {
                val longValue = asIntegerValue().toLong()
                if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) longValue.toInt() else longValue
            }
            isFloatValue -> asFloatValue().toDouble()
            isStringValue -> asStringValue().asString()
            isBinaryValue -> asBinaryValue().asByteArray()
            isArrayValue -> asArrayValue().list().map { it.toKotlinValue() }
            isMapValue -> asMapValue().map().entries.associate { (k, v) ->
                val key = k.toKotlinValue()?.toString() ?: ""
                key to v.toKotlinValue()
            }
            else -> toString()
        }
    }
}
