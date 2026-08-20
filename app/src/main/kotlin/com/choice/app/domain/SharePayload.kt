package com.choice.app.domain

const val CURRENT_SHARE_SCHEMA_VERSION = 1

data class SharedCoin(
    val schemaVersion: Int,
    val name: String,
    val choices: List<SharedChoice>,
    val weightedEnabled: Boolean,
    val avoidLastResultEnabled: Boolean,
)

data class SharedChoice(
    val text: String,
    val weight: Int?,
)

class InvalidSharePayloadException(message: String) : Exception(message)

fun encodeSharedCoin(coin: SharedCoin): String {
    val sb = StringBuilder()
    sb.append("{")
    sb.append("\"schemaVersion\":${coin.schemaVersion}")
    sb.append(",\"name\":${escapeJson(coin.name)}")
    sb.append(",\"choices\":[")
    coin.choices.forEachIndexed { index, choice ->
        if (index > 0) sb.append(",")
        sb.append("{\"text\":${escapeJson(choice.text)}")
        if (choice.weight != null) {
            sb.append(",\"weight\":${choice.weight}")
        } else {
            sb.append(",\"weight\":null")
        }
        sb.append("}")
    }
    sb.append("]")
    sb.append(",\"weightedEnabled\":${coin.weightedEnabled}")
    sb.append(",\"avoidLastResultEnabled\":${coin.avoidLastResultEnabled}")
    sb.append("}")
    return sb.toString()
}

fun decodeSharedCoin(json: String): SharedCoin {
    val parsed = parseJsonObject(json)

    val schemaVersion = parsed["schemaVersion"]?.toIntOrNull()
        ?: throw InvalidSharePayloadException("schemaVersion is required and must be a number")
    if (schemaVersion != CURRENT_SHARE_SCHEMA_VERSION) {
        throw InvalidSharePayloadException("Unsupported schema version: $schemaVersion")
    }

    val name = parsed["name"]?.removeSurrounding("\"")
        ?: throw InvalidSharePayloadException("name is required")
    if (name.isBlank()) {
        throw InvalidSharePayloadException("name must not be blank")
    }
    if (name.length > 40) {
        throw InvalidSharePayloadException("name must not exceed 40 characters")
    }

    val choicesRaw = parsed["choices"]
        ?: throw InvalidSharePayloadException("choices are required")
    val choicesArray = parseJsonArray(choicesRaw)
    if (choicesArray.size < 2) {
        throw InvalidSharePayloadException("At least 2 choices are required")
    }

    val choices = choicesArray.map { choiceStr ->
        val choiceObj = parseJsonObject(choiceStr.trim())
        val text = choiceObj["text"]?.removeSurrounding("\"")
            ?: throw InvalidSharePayloadException("choice text is required")
        if (text.isBlank()) {
            throw InvalidSharePayloadException("choice text must not be blank")
        }
        if (text.length > 60) {
            throw InvalidSharePayloadException("choice text must not exceed 60 characters")
        }
        val weightRaw = choiceObj["weight"]
        val weight = if (weightRaw == null || weightRaw == "null") {
            null
        } else {
            val w = weightRaw.toIntOrNull()
                ?: throw InvalidSharePayloadException("weight must be a whole number")
            if (w <= 0) {
                throw InvalidSharePayloadException("weight must be a positive whole number")
            }
            w
        }
        SharedChoice(text = text, weight = weight)
    }

    val weightedEnabled = parseBoolean(parsed["weightedEnabled"]
        ?: throw InvalidSharePayloadException("weightedEnabled is required"))
    val avoidLastResultEnabled = parseBoolean(parsed["avoidLastResultEnabled"]
        ?: throw InvalidSharePayloadException("avoidLastResultEnabled is required"))

    return SharedCoin(
        schemaVersion = schemaVersion,
        name = name,
        choices = choices,
        weightedEnabled = weightedEnabled,
        avoidLastResultEnabled = avoidLastResultEnabled,
    )
}

private fun parseJsonObject(json: String): Map<String, String> {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
        throw InvalidSharePayloadException("Invalid JSON object")
    }
    val inner = trimmed.substring(1, trimmed.length - 1).trim()
    if (inner.isEmpty()) return emptyMap()

    val result = mutableMapOf<String, String>()
    var i = 0
    while (i < inner.length) {
        while (i < inner.length && inner[i].isWhitespace()) i++
        if (i >= inner.length) break

        if (inner[i] != '"') {
            throw InvalidSharePayloadException("Expected key at position $i")
        }
        i++
        val keyStart = i
        while (i < inner.length && inner[i] != '"') {
            if (inner[i] == '\\') i++
            i++
        }
        val key = inner.substring(keyStart, i)
        i++

        while (i < inner.length && inner[i] != ':') i++
        i++
        while (i < inner.length && inner[i].isWhitespace()) i++

        val valueStart = i
        if (i < inner.length && inner[i] == '"') {
            i++
            while (i < inner.length && inner[i] != '"') {
                if (inner[i] == '\\') i++
                i++
            }
            i++
            result[key] = inner.substring(valueStart, i)
        } else if (i < inner.length && (inner[i] == '{' || inner[i] == '[')) {
            val bracket = inner[i]
            val close = if (bracket == '{') '}' else ']'
            var depth = 1
            i++
            while (i < inner.length && depth > 0) {
                if (inner[i] == bracket) depth++
                else if (inner[i] == close) depth--
                else if (inner[i] == '"') {
                    i++
                    while (i < inner.length && inner[i] != '"') {
                        if (inner[i] == '\\') i++
                        i++
                    }
                }
                i++
            }
            result[key] = inner.substring(valueStart, i)
        } else {
            while (i < inner.length && inner[i] != ',' && inner[i] != '}') i++
            result[key] = inner.substring(valueStart, i).trim()
        }

        while (i < inner.length && (inner[i].isWhitespace() || inner[i] == ',')) i++
    }
    return result
}

private fun parseJsonArray(arrayStr: String): List<String> {
    val trimmed = arrayStr.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
        throw InvalidSharePayloadException("Invalid JSON array")
    }
    val inner = trimmed.substring(1, trimmed.length - 1).trim()
    if (inner.isEmpty()) return emptyList()

    val items = mutableListOf<String>()
    var i = 0
    while (i < inner.length) {
        while (i < inner.length && inner[i].isWhitespace()) i++
        if (i >= inner.length) break

        val start = i
        if (inner[i] == '"') {
            i++
            while (i < inner.length && inner[i] != '"') {
                if (inner[i] == '\\') i++
                i++
            }
            i++
            items.add(inner.substring(start, i))
        } else if (inner[i] == '{') {
            var depth = 1
            i++
            while (i < inner.length && depth > 0) {
                when (inner[i]) {
                    '{' -> depth++
                    '}' -> depth--
                    '"' -> {
                        i++
                        while (i < inner.length && inner[i] != '"') {
                            if (inner[i] == '\\') i++
                            i++
                        }
                    }
                }
                i++
            }
            items.add(inner.substring(start, i))
        } else {
            while (i < inner.length && inner[i] != ',' && inner[i] != ']') i++
            items.add(inner.substring(start, i).trim())
        }

        while (i < inner.length && (inner[i].isWhitespace() || inner[i] == ',')) i++
    }
    return items
}

private fun parseBoolean(value: String): Boolean {
    return when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw InvalidSharePayloadException("Invalid boolean value: $value")
    }
}

private fun escapeJson(value: String): String {
    val sb = StringBuilder("\"")
    for (c in value) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
    }
    sb.append("\"")
    return sb.toString()
}
