/*
 * Copyright 2021 Expedia, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expediagroup.graphql.plugin.client

import graphql.schema.idl.SchemaParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.runBlocking
import java.net.UnknownHostException

/**
 * Downloads GraphQL SDL from the specified endpoint and verifies whether the result is a valid GraphQL schema.
 */
fun downloadSchema(
    endpoint: String,
    httpHeaders: Map<String, Any> = emptyMap(),
    connectTimeout: Long = 5_000,
    readTimeout: Long = 15_000
): String = HttpClient(engineFactory = Apache) {
    install(HttpTimeout) {
        connectTimeoutMillis = connectTimeout
        requestTimeoutMillis = readTimeout
    }
}.use { client ->
    runBlocking {
        val sdl = try {
            client.get<String>(urlString = endpoint) {
                httpHeaders.forEach { (name, value) ->
                    header(name, value)
                }
            }
        } catch (e: Throwable) {
            when (e) {
                is ClientRequestException, is HttpRequestTimeoutException, is UnknownHostException -> throw e
                else -> throw RuntimeException("Unable to download SDL from specified endpoint=$endpoint", e)
            }
        }
        SchemaParser().parse(sdl)
        sdl
    }
}
