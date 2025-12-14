#!/usr/bin/env kotlin

@file:DependsOn("io.ktor:ktor-client-core:2.3.7")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.7")

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Testing PocketBase s_profiles connection...")
    
    val pb = PocketBase("https://bside.pockethost.io")
    
    try {
        // Test 1: List profiles (should require auth, so expect 403 or empty)
        println("\n[Test 1] Listing profiles without auth...")
        val result = pb.collection("s_profiles").getList(QueryOptions(perPage = 1))
        println("✓ Collection exists! Response: $result")
        
    } catch (e: Exception) {
        println("✗ Error: ${e.message}")
        if (e.message?.contains("404") == true) {
            println("  → Collection NOT found on server")
        } else if (e.message?.contains("403") == true) {
            println("  → Collection exists but requires auth (expected)")
        }
    }
    
    println("\n[Test 2] Checking collection schema...")
    try {
        val collections = pb.collections.getFullList()
        val profileCollection = collections.find { it["name"] == "s_profiles" }
        
        if (profileCollection != null) {
            println("✓ s_profiles collection found!")
            println("  Fields: ${profileCollection["schema"]}")
        } else {
            println("✗ s_profiles NOT found in collections list")
        }
    } catch (e: Exception) {
        println("✗ Could not fetch collections: ${e.message}")
    }
}
