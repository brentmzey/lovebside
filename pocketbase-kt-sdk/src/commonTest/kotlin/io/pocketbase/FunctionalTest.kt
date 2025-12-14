package io.pocketbase

import io.pocketbase.functional.catchRequest
import io.pocketbase.types.Profile
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import arrow.core.Either

class FunctionalTest {

    @Test
    fun testDomainModelInstantiation() {
        val now = Clock.System.now()
        val profile = Profile(
            userId = "abc",
            firstName = "John",
            lastName = "Doe",
            birthDate = now,
            seeking = "Friendship"
        )
        assertTrue(profile.firstName == "John")
    }

    @Test
    fun testCatchRequestSuccess() = runTest {
        val result = catchRequest {
            "success"
        }
        assertTrue(result.isRight())
        assertTrue(result.getOrNull() == "success")
    }

    @Test
    fun testCatchRequestFailure() = runTest {
        val result = catchRequest {
            throw Exception("fail")
        }
        assertTrue(result.isLeft())
    }
}
