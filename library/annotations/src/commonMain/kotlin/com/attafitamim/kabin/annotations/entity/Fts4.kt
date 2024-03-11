package com.attafitamim.kabin.annotations.entity

import com.attafitamim.kabin.annotations.index.Index
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Fts4(
    //val tokenizer: String = TOKENIZER_SIMPLE,
    val tokenizerArgs: Array<String> = [],
    val contentEntity: KClass<*> = Any::class,
    val languageId: String = "",
    //val matchInfo: MatchInfo = MatchInfo.FTS4,
    val notIndexed: Array<String> = [],
    val prefix: IntArray = [],
    val order: Index.Order = Index.Order.ASC
)
