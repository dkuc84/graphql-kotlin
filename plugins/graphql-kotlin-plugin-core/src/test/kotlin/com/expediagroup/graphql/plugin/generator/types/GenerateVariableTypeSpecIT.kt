package com.expediagroup.graphql.plugin.generator.types

import com.expediagroup.graphql.plugin.generator.verifyGraphQLClientGeneration
import org.junit.jupiter.api.Test

class GenerateVariableTypeSpecIT {

    @Test
    fun `verify query with variables is correctly generated`() {
        // KT-2425 workaround to escape $ in string templates - we need to escape the escaped $
        val expected = """
            package com.expediagroup.graphql.plugin.generator.integration

            import com.expediagroup.graphql.client.GraphQLClient
            import com.expediagroup.graphql.client.GraphQLResult
            import kotlin.Boolean
            import kotlin.Float
            import kotlin.String

            const val TEST_QUERY_WITH_VARIABLES: String =
                "query TestQueryWithVariables(${'$'}{'${'$'}'}criteria: SimpleInput) {\n  inputObjectQuery(criteria: ${'$'}{'${'$'}'}criteria)\n}"

            class TestQueryWithVariables(
              private val graphQLClient: GraphQLClient
            ) {
              suspend fun testQueryWithVariables(variables: TestQueryWithVariables.Variables):
                  GraphQLResult<TestQueryWithVariables.TestQueryWithVariablesResult> =
                  graphQLClient.executeOperation(TEST_QUERY_WITH_VARIABLES, "TestQueryWithVariables", variables)

              data class Variables(
                val criteria: TestQueryWithVariables.SimpleInput?
              )

              /**
               * Test input object
               */
              data class SimpleInput(
                /**
                 * Minimum value for test criteria
                 */
                val min: Float?,
                /**
                 * Maximum value for test criteria
                 */
                val max: Float?
              )

              data class TestQueryWithVariablesResult(
                /**
                 * Query that accepts some input arguments
                 */
                val inputObjectQuery: Boolean?
              )
            }
        """.trimIndent()
        val query = """
            query TestQueryWithVariables(${'$'}criteria: SimpleInput) {
              inputObjectQuery(criteria: ${'$'}criteria)
            }
        """.trimIndent()
        verifyGraphQLClientGeneration(query, expected)
    }
}
