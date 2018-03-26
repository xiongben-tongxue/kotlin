/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.js.backend.ast.*

class IrElementToJsExpressionTransformer : BaseIrElementToJsNodeTransformer<JsExpression, JsGenerationContext> {
    override fun visitExpressionBody(body: IrExpressionBody, context: JsGenerationContext): JsExpression {
        return body.expression.accept(this, context)
    }

    override fun <T> visitConst(expression: IrConst<T>, context: JsGenerationContext): JsExpression {
        // TODO: support all cases
        return JsStringLiteral(expression.value.toString())
    }

    override fun visitGetField(expression: IrGetField, context: JsGenerationContext): JsExpression {
        return JsNameRef(expression.symbol.name.asString(), expression.receiver?.accept(this, context))
    }

    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsExpression {
        val dest = JsNameRef(expression.symbol.name.asString(), expression.receiver?.accept(this, context))
        val source = expression.value.accept(this, context)
        return jsAssignment(dest, source)
    }

    override fun visitGetValue(expression: IrGetValue, context: JsGenerationContext): JsExpression {

        return if (expression.symbol.isSpecial) {
            context.getSpecialRefForName(expression.symbol.name)
        } else {
            JsNameRef(context.getNameForSymbol(expression.symbol))
        }

    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsExpression {
        val classNameRef = expression.symbol.owner.descriptor.constructedClass.name.toJsName().makeRef()
        val callFuncRef = JsNameRef(Namer.CALL_FUNCTION, classNameRef)
        val arguments = context.translateCallArguments(expression, expression.symbol.parameterCount)
        return JsInvocation(callFuncRef, listOf(JsThisRef()) + arguments)
    }

    override fun visitCall(expression: IrCall, context: JsGenerationContext): JsExpression {
        // TODO rewrite more accurately, right now it just copy-pasted and adopted from old version
        // TODO support:
        // * ir intrinsics
        // * js be intrinsics
        // * js function
        // * getters and setters
        // * binary and unary operations

        val symbol = expression.symbol

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, context)
        val extensionReceiver = expression.extensionReceiver?.accept(this, context)

        // TODO sanitize name
        val symbolName = (symbol.owner as IrSimpleFunction).name.asString()
        val ref = if (dispatchReceiver != null) JsNameRef(symbolName, dispatchReceiver) else JsNameRef(symbolName)

        val arguments = context.translateCallArguments(expression, expression.symbol.parameterCount)


        if (symbol is IrConstructorSymbol && symbol.isPrimary) {
            return JsNew(JsNameRef((symbol.owner.parent as IrClass).name.asString()), arguments)
        }

        return JsInvocation(ref, extensionReceiver?.let { listOf(extensionReceiver) + arguments } ?: arguments)
    }
}