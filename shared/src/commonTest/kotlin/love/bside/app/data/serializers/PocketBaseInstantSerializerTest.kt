package love.bside.app.data.serializers

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PocketBaseInstantSerializerTest {

    @Serializable
    data class TestWrapper(
        @Serializable(with = PocketBaseInstantSerializer::class)
        val date: Instant
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test deserializes valid PocketBase ISO timestamp`() {
        // Given
        val jsonString = """{"date": "2024-01-01 12:00:00.000Z"}"""

        // When
        val result = json.decodeFromString<TestWrapper>(jsonString)

        // Then
        assertEquals(1704110400000L, result.date.toEpochMilliseconds())
    }

    @Test
    fun `test deserializes space instead of T`() {
        // Given (PocketBase sometimes returns space separator)
        val jsonString = """{"date": "2024-01-01 12:00:00.000Z"}"""
        val spaceString = """{"date": "2024-01-01 12:00:00.000Z"}""".replace("T", " ")

        // When
        val result = json.decodeFromString<TestWrapper>(spaceString)

        // Then
        assertEquals(1704110400000L, result.date.toEpochMilliseconds())
    }
    
    @Test
    fun `test deserializes missing Z`() {
        // Given (PocketBase sometimes returns UTC without Z suffix)
        val jsonString = """{"date": "2024-01-01 12:00:00.000"}"""

        // When
        val result = json.decodeFromString<TestWrapper>(jsonString)

        // Then
        assertEquals(1704110400000L, result.date.toEpochMilliseconds())
    }
}
