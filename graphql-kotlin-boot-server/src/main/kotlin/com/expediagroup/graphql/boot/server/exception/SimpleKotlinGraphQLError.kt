/*
 * Copyright 2019 Expedia, Inc
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

package com.expediagroup.graphql.boot.server.exception

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation

open class SimpleKotlinGraphQLError(
    private val exception: Throwable,
    private val errorType: ErrorType
) : GraphQLError {
    override fun getErrorType(): ErrorClassification = errorType

    override fun getLocations(): List<SourceLocation> = emptyList()

    override fun getMessage(): String = "Exception while running code outside of data handler: ${exception.message}"

    override fun getExtensions(): Map<String, Any> {
        val newExtensions = mutableMapOf<String, Any>()
        if (exception is GraphQLError && exception.extensions != null) {
            newExtensions.putAll(exception.extensions)
        }
        return newExtensions
    }
}