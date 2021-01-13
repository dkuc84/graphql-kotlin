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

package com.expediagroup.graphql.server.execution

import com.expediagroup.graphql.execution.DefaultGraphQLContext
import com.expediagroup.graphql.execution.GraphQLContext

/**
 * Factory that generates GraphQL context.
 */
interface GraphQLContextFactory<out Context : GraphQLContext, Request> {

    /**
     * Generate GraphQL context based on the incoming request and the corresponding response.
     * If no context should be generated and used in the request, return null.
     */
    fun generateContext(request: Request): Context?
}

/**
 * Default context factory that generates GraphQL context with empty concurrent map that can store any elements.
 */
class DefaultGraphQLContextFactory<T> : GraphQLContextFactory<DefaultGraphQLContext, T> {
    override fun generateContext(request: T) = DefaultGraphQLContext()
}