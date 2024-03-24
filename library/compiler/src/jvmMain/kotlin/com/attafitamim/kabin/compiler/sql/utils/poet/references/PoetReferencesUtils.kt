package com.attafitamim.kabin.compiler.sql.utils.poet.references

import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.squareup.kotlinpoet.TypeName

fun String.asPropertyName(): String =
    asClassName().toCamelCase()

fun String.asClassName(): String {
    val parts = split(SYMBOL_ACCESS_SIGN)

    return buildString {
        parts.forEach { part ->
            var cleanPart = part

            if (cleanPart.contains("`", true)) {
                cleanPart = cleanPart.replace("`", "")
            }

            if (cleanPart.contains("<")) {
                val cleanParts = cleanPart
                    .removeSuffix(">")
                    .split("<")

                cleanPart = cleanParts.joinToString(
                    "",
                    transform = String::toPascalCase
                )
            }

            if (cleanPart.contains(">")) {
                cleanPart = cleanPart.replace(">", "")
            }

            if (cleanPart.contains("?")) {
                cleanPart = cleanPart.replace("?", "Optional")
            }

            append(cleanPart.toPascalCase())
        }
    }
}
fun TypeName.asPropertyName(): String =
    asClassName().toCamelCase()

fun TypeName.asClassName(): String =
    toString().asClassName()

fun ColumnAdapterReference.getClassName() = buildString {
    append(
        kotlinType.asClassName(),
        affinityType.asClassName(),
        "Adapter"
    )
}

fun ColumnAdapterReference.getPropertyName() = getClassName().toCamelCase()

fun MapperReference.getPropertyName(options: KabinOptions): String {
    val className = returnType.asClassName()
    val classNameWithSuffix = buildString {
        append(className)
        append(options.getOrDefault(KabinOptions.Key.ENTITY_MAPPER_SUFFIX))
    }

    return classNameWithSuffix.toCamelCase()
}
