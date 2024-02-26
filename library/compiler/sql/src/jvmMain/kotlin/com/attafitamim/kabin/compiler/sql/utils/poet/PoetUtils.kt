package com.attafitamim.kabin.compiler.sql.utils.poet

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import java.io.OutputStream
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

fun FileSpec.writeToFile(outputStream: OutputStream) = outputStream.use { stream ->
    stream.writer().use(::writeTo)
}

// TODO: refactor this
fun FunSpec.Builder.addReturnString(value: String?) =
    when (value) {
        null -> addStatement("return null")
        else -> addStatement("return %S", value)
    }

// TODO: refactor this
fun FunSpec.Builder.addReturnListOf(values: List<String>?) =
    when (values) {
        null -> addStatement("return null")
        else -> addStatement("return listOf(${values.joinToString { 
            "%S"
        }})", *values.toTypedArray())
    }

inline fun <reified T : Any, reified V : Any> KProperty1<T, V?>.buildSpec(): PropertySpec.Builder =
    PropertySpec.builder(
        name,
        returnType.asTypeName()
    )

inline fun <reified V : Any?> KProperty.Getter<V>.buildSpec(): FunSpec.Builder = FunSpec
    .getterBuilder()

inline fun <reified T : Any, reified V : Any?> KProperty1<T, V>.asStringGetterPropertySpec(
    value: String?
): PropertySpec {
    val getterSpec = getter.buildSpec()
        .addReturnString(value)
        .build()

    return buildSpec()
        .addModifiers(com.squareup.kotlinpoet.KModifier.OVERRIDE)
        .getter(getterSpec)
        .build()
}

inline fun <reified T : Any, reified V : Any?> KProperty1<T, V>.asListGetterPropertySpec(
    values: List<String>?
): PropertySpec {
    val getterSpec = getter.buildSpec()
        .addReturnListOf(values)
        .build()

    return buildSpec()
        .addModifiers(com.squareup.kotlinpoet.KModifier.OVERRIDE)
        .getter(getterSpec)
        .build()
}
