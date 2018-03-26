/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsExpressionTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsStatementTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.toJsName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.Name

class JsGenerationContext {
    fun newDeclaration(scope: JsScope): JsGenerationContext {
        return JsGenerationContext(this, JsBlock(), scope)
    }

    val currentBlock: JsBlock
    val currentScope: JsScope
    val parent: JsGenerationContext?
    private val staticContext: JsStaticContext
    private val program: JsProgram

    constructor(rootScope: JsRootScope) {

        this.parent = null
        this.program = rootScope.program
        this.staticContext = JsStaticContext(rootScope, program.globalBlock, SimpleNameGenerator())
        this.currentScope = rootScope
        this.currentBlock = program.globalBlock
    }

    constructor(parent: JsGenerationContext, block: JsBlock, scope: JsScope) {
        this.parent = parent
        this.program = parent.program
        this.staticContext = parent.staticContext
        this.currentBlock = block
        this.currentScope = scope
    }

    fun translateFunction(declaration: IrFunction, name: Name?): JsFunction {
        val functionScope = JsFunctionScope(currentScope, "scope for ${name ?: "annon"}")
        val functionContext = newDeclaration(functionScope)
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
                function.addParameter(staticContext.getSpecialNameString(it.name.asString()))
            } else {
                function.addParameter(it.name.asString())
            }
        }

        return function
    }

    fun translateCallArguments(
        expression: IrMemberAccessExpression,
        parameterCount: Int
    ): List<JsExpression> {
        val transformer = IrElementToJsExpressionTransformer()
        // TODO: map to?
        return (0 until parameterCount).map {
            val argument = expression.getValueArgument(it)
            if (argument != null) {
                argument.accept(transformer, this)
            } else {
                JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
            }
        }
    }

    fun getNameForDescriptor(descriptor: DeclarationDescriptor): JsName = staticContext.getNameForDescriptor(descriptor)
    fun getNameForSymbol(symbol: IrSymbol): JsName = staticContext.getNameForSymbol(symbol)
    fun getSpecialRefForName(name: Name): JsExpression = staticContext.getSpecialRefForName(name)
}