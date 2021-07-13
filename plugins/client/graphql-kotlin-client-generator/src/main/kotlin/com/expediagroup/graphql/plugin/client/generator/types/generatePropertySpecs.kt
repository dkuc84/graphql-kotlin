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

package com.expediagroup.graphql.plugin.client.generator.types

import com.expediagroup.graphql.plugin.client.generator.GraphQLClientGeneratorContext
import com.expediagroup.graphql.plugin.client.generator.GraphQLScalar
import com.expediagroup.graphql.plugin.client.generator.GraphQLSerializer
import com.expediagroup.graphql.plugin.client.generator.exceptions.DeprecatedFieldsSelectedException
import com.expediagroup.graphql.plugin.client.generator.exceptions.InvalidSelectionSetException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import graphql.Directives.DeprecatedDirective
import graphql.language.Field
import graphql.language.FieldDefinition
import graphql.language.NonNullType
import graphql.language.SelectionSet
import graphql.language.StringValue
import kotlinx.serialization.Serializable
import java.lang.reflect.Parameter

/**
 * Generate [PropertySpec]s from the field definitions and selection set.
 */
internal fun generatePropertySpecs(
    context: GraphQLClientGeneratorContext,
    objectName: String,
    selectionSet: SelectionSet,
    fieldDefinitions: List<FieldDefinition>,
    abstract: Boolean = false
): List<PropertySpec> = selectionSet.getSelectionsOfType(Field::class.java)
    .filterNot { it.name == "__typename" }
    .map { selectedField ->
        val fieldDefinition = fieldDefinitions.find { it.name == selectedField.name }
            ?: throw InvalidSelectionSetException(context.operationName, selectedField.name, objectName)

        val nullable = fieldDefinition.type !is NonNullType
        val kotlinFieldType = generateTypeName(context, fieldDefinition.type, selectedField.selectionSet)
        val fieldName = selectedField.alias ?: fieldDefinition.name

        val propertySpecBuilder = PropertySpec.builder(fieldName, kotlinFieldType.copy(nullable = nullable))
        if (!abstract) {
            propertySpecBuilder.initializer(fieldName)
            if (context.isCustomScalar(kotlinFieldType)) {
                if (context.serializer == GraphQLSerializer.JACKSON) {
                    val serializers = context.scalarClassToConverterTypeSpecs[kotlinFieldType]!!
                    propertySpecBuilder.addAnnotation(
                        AnnotationSpec.builder(JsonSerialize::class)
                            .addMember("converter = %T::class", ClassName("${context.packageName}.scalars", serializers[0].name!!))
                            .build()
                    )
                    propertySpecBuilder.addAnnotation(
                        AnnotationSpec.builder(JsonDeserialize::class)
                            .addMember("converter = %T::class", ClassName("${context.packageName}.scalars", serializers[1].name!!))
                            .build()
                    )
                } else {
                    val serializers = context.scalarClassToConverterTypeSpecs[kotlinFieldType]!!
                    propertySpecBuilder.addAnnotation(
                        AnnotationSpec.builder(Serializable::class)
                            .addMember("with = %T::class", ClassName("${context.packageName}.scalars", serializers[0].name!!))
                            .build()
                    )
                }
            }
            // TODO handle list
//            } else if (kotlinFieldType is ParameterizedTypeName && context.isCustomScalar(kotlinFieldType.rawType) && context.serializer == GraphQLSerializer.JACKSON) {
//                // handle list annotation in JACKSON
//                val serializers = context.scalarClassToConverterTypeSpecs[kotlinFieldType.rawType]!!
//                propertySpecBuilder.addAnnotation(
//                    AnnotationSpec.builder(JsonSerialize::class)
//                        .addMember("converter = %T::class", serializers[0])
//                        .build()
//                )
//                propertySpecBuilder.addAnnotation(
//                    AnnotationSpec.builder(JsonDeserialize::class)
//                        .addMember("converter = %T::class", serializers[1])
//                        .build()
//                )
//            }

        } else {
            propertySpecBuilder.addModifiers(KModifier.ABSTRACT)
        }
        val deprecatedDirective = fieldDefinition.getDirectives(DeprecatedDirective.name).firstOrNull()
        if (deprecatedDirective != null) {
            if (!context.allowDeprecated) {
                throw DeprecatedFieldsSelectedException(context.operationName, selectedField.name, objectName)
            } else {
                val deprecatedReason = deprecatedDirective.getArgument("reason")?.value as? StringValue
                val reason = deprecatedReason?.value ?: "no longer supported"
                propertySpecBuilder.addAnnotation(
                    AnnotationSpec.builder(Deprecated::class)
                        .addMember("message = %S", reason)
                        .build()
                )
            }
        }
        fieldDefinition.description?.content?.let { kdoc ->
            propertySpecBuilder.addKdoc("%L", kdoc)
        }
        propertySpecBuilder.build()
    }
