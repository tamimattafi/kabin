package com.attafitamim.kabin.compiler.sql.utils.poet

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.OutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

fun List<KSType>.asTypeNames(): List<TypeName> = map(KSType::toTypeName)

fun KSFunctionDeclaration.buildSpec(): FunSpec.Builder {
    val builder = FunSpec.builder(simpleName.asString())
        .addParameters(parameters.asSpecs())


    val returnType = returnType?.resolve()
    if (returnType != null) {
        builder.returns(returnType.toTypeName())
    }

    return builder
}

fun KSValueParameter.buildSpec(): ParameterSpec.Builder =
    ParameterSpec.builder(
        requireNotNull(name?.asString()),
        type.toTypeName(),
        modifiers = type.modifiers.asKModifiers()
    )

fun List<KSValueParameter>.asSpecs() = map { parameter ->
    parameter.buildSpec().build()
}

// TODO: refactor this
fun Modifier.asKModifier(): KModifier = enumValueOf(name)

fun Collection<Modifier>.asKModifiers(): Collection<KModifier> = map(Modifier::asKModifier)

fun FileSpec.writeToFile(outputStream: OutputStream) = outputStream.use { stream ->
    stream.writer().use(::writeTo)
}

inline fun <reified T : Any> FunSpec.Builder.addParameter(
    clazz: KClass<T> = T::class
): FunSpec.Builder {
    val parameterName = parameterName<T>()
    return addParameter(parameterName, clazz)
}

inline fun <reified T : Any> parameterName(
    clazz: KClass<T> = T::class
) : String = requireNotNull(clazz.simpleName).lowercase()

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
        .addModifiers(KModifier.OVERRIDE)
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
        .addModifiers(KModifier.OVERRIDE)
        .getter(getterSpec)
        .build()
}
