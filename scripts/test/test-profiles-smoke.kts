#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-core-jvm:2.3.7")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:2.3.7")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:2.3.7")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.7")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Quick smoke test to verify s_profiles collection is accessible
 */
fun main() = runBlocking {
    println("🧪 Testing Pockethost s_profiles collection...")
    println("=" .repeat(50))
    
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    try {
        println("\n[Test 1] GET /api/collections/s_profiles/records")
        val response: HttpResponse = client.get("https://bside.pockethost.io/api/collections/s_profiles/records") {
            parameter("perPage", 1)
        }
        
        println("Status: ${response.status}")
        println("Response: ${response.bodyAsText()}")
        
        when (response.status.value) {
            200 -> println("\n✅ SUCCESS: Collection exists and is accessible!")
            403 -> println("\n✅ EXPECTED: Collection exists but requires auth (correct!)")
            404 -> println("\n❌ FAILED: Collection not found - check server")
            else -> println("\n⚠️  Unexpected status")
        }
        
    } catch (e: Exception) {
        println("\n❌ ERROR: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
