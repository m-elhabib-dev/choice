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

/**
 * One case per validation branch below. Domain code stays free of Android resource dependencies
 * (Principle VI) — the Composable resolves the final localized message via `stringResource`, the
 * same pattern as `CoinEditError` (see `ImportCoinScreen.kt`).
 */
sealed class SharePayloadError {
    data object MalformedJson : SharePayloadError()
    data object MissingSchemaVersion : SharePayloadError()
    data class UnsupportedSchemaVersion(val version: Int) : SharePayloadError()
    data object NameRequired : SharePayloadError()
    data object NameBlank : SharePayloadError()
    data class NameTooLong(val maxLength: Int) : SharePayloadError()
    data object ChoicesRequired : SharePayloadError()
    data object TooFewChoices : SharePayloadError()
    data object ChoiceTextRequired : SharePayloadError()
    data object ChoiceTextBlank : SharePayloadError()
    data class ChoiceTextTooLong(val maxLength: Int) : SharePayloadError()
    data object WeightNotANumber : SharePayloadError()
    data object WeightNotPositive : SharePayloadError()
    data object MissingWeightedEnabled : SharePayloadError()
    data object MissingAvoidLastResultEnabled : SharePayloadError()
    data object InvalidBoolean : SharePayloadError()
}

/**
 * [message] is a non-user-facing developer fallback only (e.g. for logs) — user-facing text is
 * always resolved from [reason] via a `@StringRes` mapping in `ImportCoinViewModel`/
 * `ImportCoinScreen`, never from this English string.
 */
class InvalidSharePayloadException(
    val reason: SharePayloadError,
    message: String,
) : Exception(message)

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
        ?: throw InvalidSharePayloadException(
            SharePayloadError.MissingSchemaVersion,
            "schemaVersion is required and must be a number",
        )
    if (schemaVersion != CURRENT_SHARE_SCHEMA_VERSION) {
        throw InvalidSharePayloadException(
            SharePayloadError.UnsupportedSchemaVersion(schemaVersion),
            "Unsupported schema version: $schemaVersion",
        )
    }

    val name = parsed["name"]?.removeSurrounding("\"")
        ?: throw InvalidSharePayloadException(SharePayloadError.NameRequired, "name is required")
    if (name.isBlank()) {
        throw InvalidSharePayloadException(SharePayloadError.NameBlank, "name must not be blank")
    }
    if (name.length > 40) {
        throw InvalidSharePayloadException(
            SharePayloadError.NameTooLong(40),
            "name must not exceed 40 characters",
        )
    }

    val choicesRaw = parsed["choices"]
        ?: throw InvalidSharePayloadException(SharePayloadError.ChoicesRequired, "choices are required")
    val choicesArray = parseJsonArray(choicesRaw)
    if (choicesArray.size < 2) {
        throw InvalidSharePayloadException(
            SharePayloadError.TooFewChoices,
            "At least 2 choices are required",
        )
    }

    val choices = choicesArray.map { choiceStr ->
        val choiceObj = parseJsonObject(choiceStr.trim())
        val text = choiceObj["text"]?.removeSurrounding("\"")
            ?: throw InvalidSharePayloadException(
                SharePayloadError.ChoiceTextRequired,
                "choice text is required",
            )
        if (text.isBlank()) {
            throw InvalidSharePayloadException(
                SharePayloadError.ChoiceTextBlank,
                "choice text must not be blank",
            )
        }
        if (text.length > 60) {
            throw InvalidSharePayloadException(
                SharePayloadError.ChoiceTextTooLong(60),
                "choice text must not exceed 60 characters",
            )
        }
        val weightRaw = choiceObj["weight"]
        val weight = if (weightRaw == null || weightRaw == "null") {
            null
        } else {
            val w = weightRaw.toIntOrNull()
                ?: throw InvalidSharePayloadException(
                    SharePayloadError.WeightNotANumber,
                    "weight must be a whole number",
                )
            if (w <= 0) {
                throw InvalidSharePayloadException(
                    SharePayloadError.WeightNotPositive,
                    "weight must be a positive whole number",
                )
            }
            w
        }
        SharedChoice(text = text, weight = weight)
    }

    val weightedEnabled = parseBoolean(
        parsed["weightedEnabled"]
            ?: throw InvalidSharePayloadException(
                SharePayloadError.MissingWeightedEnabled,
                "weightedEnabled is required",
            ),
    )
    val avoidLastResultEnabled = parseBoolean(
        parsed["avoidLastResultEnabled"]
            ?: throw InvalidSharePayloadException(
                SharePayloadError.MissingAvoidLastResultEnabled,
                "avoidLastResultEnabled is required",
            ),
    )

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
        throw InvalidSharePayloadException(SharePayloadError.MalformedJson, "Invalid JSON object")
    }
    val inner = trimmed.substring(1, trimmed.length - 1).trim()
    if (inner.isEmpty()) return emptyMap()

    val result = mutableMapOf<String, String>()
    var i = 0
    while (i < inner.length) {
        while (i < inner.length && inner[i].isWhitespace()) i++
        if (i >= inner.length) break

        if (inner[i] != '"') {
            throw InvalidSharePayloadException(SharePayloadError.MalformedJson, "Expected key at position $i")
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
        throw InvalidSharePayloadException(SharePayloadError.MalformedJson, "Invalid JSON array")
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
        else -> throw InvalidSharePayloadException(
            SharePayloadError.InvalidBoolean,
            "Invalid boolean value: $value",
        )
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
