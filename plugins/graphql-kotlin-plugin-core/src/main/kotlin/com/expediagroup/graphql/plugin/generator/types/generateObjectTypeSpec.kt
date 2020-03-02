package com.expediagroup.graphql.plugin.generator.types

import com.expediagroup.graphql.plugin.generator.GraphQLClientGeneratorContext
import com.expediagroup.graphql.plugin.generator.graphQLComments
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.SelectionSet

internal fun generateObjectTypeSpec(context: GraphQLClientGeneratorContext, parentObject: ObjectTypeDefinition, selectionSet: SelectionSet, objectNameOverride: String? = null): TypeSpec {
    // TODO what if we select same object but different fields
    //   use different key -> name + selection set
    val typeName = objectNameOverride ?: parentObject.name
    val objectTypeSpecBuilder = TypeSpec.classBuilder(typeName)
    objectTypeSpecBuilder.modifiers.add(KModifier.DATA)
    parentObject.graphQLComments()?.let { kdoc ->
        objectTypeSpecBuilder.addKdoc(kdoc)
    }

    val constructorBuilder = FunSpec.constructorBuilder()
    selectionSet.getSelectionsOfType(Field::class.java)
        .filterNot { it.name == "__typename" }
        .forEach { selectedField ->
            val fieldDefinition = parentObject.fieldDefinitions.find { it.name == selectedField.name }
                ?: throw RuntimeException("unable to find corresponding field definition ${selectedField.name} in ${parentObject.name}")
            val nullable = fieldDefinition.type !is NonNullType
            val kotlinFieldType = generateKotlinTypeName(context, fieldDefinition.type, selectedField.selectionSet)
            val fieldName = selectedField.alias ?: fieldDefinition.name

            val propertySpec = PropertySpec.builder(fieldName, kotlinFieldType.copy(nullable = nullable))
                .initializer(fieldName)
                .build()
            objectTypeSpecBuilder.addProperty(propertySpec)
            constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
        }

    selectionSet.getSelectionsOfType(FragmentSpread::class.java)
        .forEach { fragment ->
            val fragmentDefinitions = context.queryDocument.getDefinitionsOfType(FragmentDefinition::class.java)
            val fragmentDefinition = fragmentDefinitions.find { it.name == fragment.name } ?: throw RuntimeException("fragment not found")
            generateObjectTypeSpec(context, parentObject, fragmentDefinition.selectionSet)
        }

    objectTypeSpecBuilder.primaryConstructor(constructorBuilder.build())

    val objectTypeSpec = objectTypeSpecBuilder.build()
    context.typeSpecs.add(objectTypeSpec)
    return objectTypeSpec
}
