package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePayloadTest {

    @Test
    fun encodeSharedCoin_producesValidJson() {
        val coin = SharedCoin(
            schemaVersion = 1,
            name = "Breakfast",
            choices = listOf(
                SharedChoice(text = "Ful", weight = null),
                SharedChoice(text = "Eggs", weight = 2),
            ),
            weightedEnabled = true,
            avoidLastResultEnabled = false,
        )

        val json = encodeSharedCoin(coin)
        val decoded = decodeSharedCoin(json)

        assertEquals(coin, decoded)
    }

    @Test
    fun encodeSharedCoin_roundTrip_preservesAllFields() {
        val coin = SharedCoin(
            schemaVersion = 1,
            name = "Lunch",
            choices = listOf(
                SharedChoice(text = "Rice", weight = 3),
                SharedChoice(text = "Pasta", weight = null),
                SharedChoice(text = "Chicken", weight = 1),
            ),
            weightedEnabled = false,
            avoidLastResultEnabled = true,
        )

        val json = encodeSharedCoin(coin)
        val decoded = decodeSharedCoin(json)

        assertEquals(1, decoded.schemaVersion)
        assertEquals("Lunch", decoded.name)
        assertEquals(3, decoded.choices.size)
        assertEquals("Rice", decoded.choices[0].text)
        assertEquals(3, decoded.choices[0].weight)
        assertEquals("Pasta", decoded.choices[1].text)
        assertEquals(null, decoded.choices[1].weight)
        assertEquals("Chicken", decoded.choices[2].text)
        assertEquals(1, decoded.choices[2].weight)
        assertEquals(false, decoded.weightedEnabled)
        assertEquals(true, decoded.avoidLastResultEnabled)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsMalformedJson() {
        decodeSharedCoin("not valid json")
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsMissingSchemaVersion() {
        val json = """{"name":"Test","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsUnsupportedSchemaVersion() {
        val json = """{"schemaVersion":99,"name":"Test","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsMissingName() {
        val json = """{"schemaVersion":1,"choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsBlankName() {
        val json = """{"schemaVersion":1,"name":"  ","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsLessThanTwoChoices() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsBlankChoiceText() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsNonPositiveWeight() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":0},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsNegativeWeight() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":-1},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsMissingWeightedEnabled() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"avoidLastResultEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test(expected = InvalidSharePayloadException::class)
    fun decodeSharedCoin_rejectsMissingAvoidLastResultEnabled() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false}"""
        decodeSharedCoin(json)
    }

    @Test
    fun decodeSharedCoin_acceptsNullWeight() {
        val json = """{"schemaVersion":1,"name":"Test","choices":[{"text":"A","weight":null},{"text":"B","weight":null}],"weightedEnabled":false,"avoidLastResultEnabled":false}"""
        val decoded = decodeSharedCoin(json)
        assertEquals(null, decoded.choices[0].weight)
    }
}
