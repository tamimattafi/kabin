package com.attafitamim.kabin.compiler.sql.utils.poet

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.OutputStream
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.valueParameters

fun ClassName.asPropertyName() = simpleNames.joinToString("").toCamelCase()

fun ClassName.asClassName() = simpleNames.joinToString("").toPascalCase()

fun KSType.toSimpleTypeName() = toTypeName().copy(nullable = false)

fun typeInitializer(
    parameters: List<String> = emptyList(),
    isForReturn: Boolean = false
): String {
    val parameterCalls = parameters.joinToString()

    return buildString {
        if (isForReturn) {
            append("return ")
        }

        append("%T($parameterCalls)")
    }
}

fun CodeGenerator.writeType(
    className: ClassName,
    typeSpec: TypeSpec
) {
    val fileSpec = FileSpec.builder(className)
        .addType(typeSpec)
        .build()

    writeFile(className, fileSpec)
}

fun CodeGenerator.writeFile(
    className: ClassName,
    fileSpec: FileSpec
) {
    val outputFile = createNewFile(
        Dependencies(aggregating = false),
        className.packageName,
        className.simpleName
    )

    fileSpec.writeToFile(outputFile)
}

val KSDeclaration.qualifiedNameString get() = qualifiedName?.asString() ?: simpleNameString

val KSDeclaration.simpleNameString get() = simpleName.asString()

fun List<KSType>.asTypeNames(): List<TypeName> = map(KSType::toTypeName)

fun KSFunctionDeclaration.buildSpec(): FunSpec.Builder {
    val builder = FunSpec.builder(simpleName.asString())
        .addParameters(parameters.asSpecs())

    val returnType = returnType?.resolve()
    if (returnType != null) {
        builder.returns(returnType.toTypeName())
    }

    builder.addModifiers(modifiers.asKModifiers())

    return builder
}

fun KFunction<*>.buildSpec(actualName: String = name): FunSpec.Builder = FunSpec.builder(actualName)
    .addParameters(valueParameters.asKSpecs())
    .returns(returnType.asTypeName())
    .apply {
        if (isSuspend) {
            addModifiers(KModifier.SUSPEND)
        }
    }

fun KSValueParameter.buildSpec(): ParameterSpec.Builder =
    ParameterSpec.builder(
        requireNotNull(name?.asString()),
        type.toTypeName(),
        modifiers = type.modifiers.asKModifiers()
    )

fun KParameter.buildSpec(): ParameterSpec.Builder =
    ParameterSpec.builder(
        requireNotNull(name),
        type.asTypeName()
    )

fun List<KSValueParameter>.asSpecs() = map { parameter ->
    parameter.buildSpec().build()
}

fun List<KParameter>.asKSpecs() = map { parameter ->
    parameter.buildSpec().build()
}

// TODO: refactor this
fun Modifier.asKModifier(): KModifier = enumValueOf(name)

fun Collection<Modifier>.asKModifiers(): Collection<KModifier> = map(Modifier::asKModifier)

fun FileSpec.writeToFile(outputStream: OutputStream) = outputStream.use { stream ->
    stream.writer().use(::writeTo)
}

fun String.toCamelCase(): String = buildString {
    append(this@toCamelCase.first().lowercase())
    append(this@toCamelCase, 1, this@toCamelCase.length)
}

fun String.toPascalCase(): String = buildString {
    append(this@toPascalCase.first().uppercase())
    append(this@toPascalCase, 1, this@toPascalCase.length)
}

inline fun <reified T : Any, reified V : Any> KProperty1<T, V?>.buildSpec(): PropertySpec.Builder =
    PropertySpec.builder(
        name,
        returnType.asTypeName()
    )

inline fun <reified T : Any, reified V : Any> KProperty1<T, V?>.parameterBuildSpec(): ParameterSpec.Builder =
    ParameterSpec.builder(
        name,
        returnType.asTypeName()
    )

inline fun <reified V : Any?> KProperty.Getter<V>.buildSpec(): FunSpec.Builder = FunSpec
    .getterBuilder()
