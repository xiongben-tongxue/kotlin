/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.Name

// TODO don't use JsDynamicScope
val dummyScope = JsDynamicScope

fun Name.toJsName() =
    // TODO sanitize
    dummyScope.declareName(asString())

fun jsVar(name: Name, initializer: IrExpression?, context: JsGenerationContext): JsVars {
    val jsInitializer = initializer?.accept(IrElementToJsExpressionTransformer(), context)
    return JsVars(JsVars.JsVar(name.toJsName(), jsInitializer))
}

fun jsAssignment(left: JsExpression, right: JsExpression) = JsBinaryOperation(JsBinaryOperator.ASG, left, right)

fun prototypeOf(classNameRef: JsExpression) = JsNameRef(Namer.PROTOTYPE_NAME, classNameRef)

fun translateFunction(declaration: IrFunction, name: Name?, context: JsGenerationContext): JsFunction {
    val functionScope = JsFunctionScope(context.currentScope, "scope for ${name ?: "annon"}")
    val functionContext = context.newDeclaration(functionScope)
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()
    val function = JsFunction(functionScope, body, "member function ${name ?: "annon"}")

    function.name = name?.toJsName()

    fun JsFunction.addParameter(parameterName: String) {
        val parameter = function.scope.declareName(parameterName)
        parameters.add(JsParameter(parameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter("\$receiver") }
    declaration.valueParameters.forEach {
        if (it.name.isSpecial) {
            function.addParameter(context.staticContext.getSpecialNameString(it.name.asString()))
        } else {
            function.addParameter(it.name.asString())
        }
    }

    return function
}

fun translateCallArguments(
    expression: IrMemberAccessExpression,
    parameterCount: Int,
    context: JsGenerationContext
): List<JsExpression> {
    val transformer = IrElementToJsExpressionTransformer()
    // TODO: map to?
    return (0 until parameterCount).map {
        val argument = expression.getValueArgument(it)
        argument?.accept(transformer, context) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
    }
}