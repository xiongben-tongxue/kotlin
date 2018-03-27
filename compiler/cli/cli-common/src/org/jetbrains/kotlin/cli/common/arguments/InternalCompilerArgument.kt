/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageFeature

sealed class InternalArgument

data class ManualLanguageFeatureSetting(val languageFeature: LanguageFeature, val state: LanguageFeature.State) : InternalArgument()

class InternalArgumentsParser(private val collector: MessageCollector) {
    fun parseInternalArgument(arg: String): InternalArgument? {
        // Currently, we have only internal arguments of form '-XX:+LanguageFeature' or '-XX:-LanguageFeature',
        // which enable or disable corresponding LanguageFeature.

        // '+LanguageFeature' or '-LanguageFeature'
        val argumentWithoutPrefix = arg.removePrefix(INTERNAL_ARGUMENT_PREFIX)

        // '+' or '-'
        val modificator = argumentWithoutPrefix.firstOrNull()
        val languageFeatureState = when (modificator) {
            '+' -> LanguageFeature.State.ENABLED

            '-' -> LanguageFeature.State.DISABLED

            else -> {
                collector.report(CompilerMessageSeverity.ERROR, "Incorrect internal argument syntax, missing modificator: $arg")
                return null
            }
        }

        val languageFeatureName = argumentWithoutPrefix.substring(1)

        if (languageFeatureName.isEmpty()) {
            collector.report(CompilerMessageSeverity.ERROR, "Empty language feature name for internal argument '$arg'")
            return null
        }

        val languageFeature = LanguageFeature.fromString(languageFeatureName)

        if (languageFeature == null) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Unknown language feature '$languageFeatureName' in passed internal argument '$arg'"
            )
            return null
        }

        return ManualLanguageFeatureSetting(languageFeature, languageFeatureState)
    }
}