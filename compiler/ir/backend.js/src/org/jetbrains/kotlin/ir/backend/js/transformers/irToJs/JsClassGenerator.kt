/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.backend.ast.*

class JsClassGenerator(val irClass: IrClass, val context: JsGenerationContext) {

    private val metadataLiteral = JsObjectLiteral(true)
    private val classNameRef = irClass.name.toJsName().makeRef()
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsBlock()

    fun generate(): JsStatement {
        maybeGeneratePrimaryConstructor()

        irClass.declarations.forEach {
            if (it is IrFunction) {
                if (it.symbol.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                    it.symbol.modality != Modality.ABSTRACT
                ) {
                    val statement = generateForInnerDeclaration(it)
                    if (it is IrConstructor) {
                        generateFieldInitializers((statement as JsExpressionStatement).expression as JsFunction)
                    }
                    classBlock.statements += statement
                }
            }
            if (it is IrConstructor) {
                classBlock.statements += generateInheritanceCode()
            }
        }

        classBlock.statements += generateClassMetadata()
        return classBlock
    }

    private fun generateFieldInitializers(function: JsFunction) {
        val currentBlock = function.body

        val expressionTransformer = IrElementToJsExpressionTransformer()

        irClass.declarations.forEach {
            if (it is IrField) {
                val fieldName = it.name.toJsName()
                val initExpression =
                    it.initializer?.accept(expressionTransformer, context) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
                currentBlock.statements += jsAssignment(JsNameRef(fieldName, JsThisRef()), initExpression).makeStmt()
            }
        }
    }

    private fun maybeGeneratePrimaryConstructor() {
        if (!irClass.declarations.any { it is IrConstructor }) {
            val func = JsFunction(JsFunctionScope(context.currentScope, "Ctor for ${irClass.name}"), JsBlock(), "Ctor for ${irClass.name}")
            func.name = irClass.name.toJsName()
            generateFieldInitializers(func)
            classBlock.statements += func.makeStmt()
            classBlock.statements += generateInheritanceCode()
        }
    }

    private fun generateInheritanceCode(): List<JsStatement> {
        val baseClass = irClass.superClasses.first { it.kind != ClassKind.INTERFACE }
        if (baseClass.isAny) {
            return emptyList()
        }

        val createCall = jsAssignment(
            classPrototypeRef, JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototypeOf(baseClass.owner.name.toJsName().makeRef()))
        ).makeStmt()

        val ctorAssign = jsAssignment(JsNameRef(Namer.CONSTRUCTOR_NAME, classPrototypeRef), classNameRef).makeStmt()

        return listOf(createCall, ctorAssign)
    }

    inner class IrInnerDeclarationToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: JsGenerationContext): JsStatement {
            val memberName = data.getNameForSymbol(declaration.symbol)
            val memberRef = JsNameRef(memberName, classPrototypeRef)
            val memberBody = data.translateFunction(declaration, name = null)
            val assignment = jsAssignment(memberRef, memberBody)

            return assignment.makeStmt()

        }

        override fun visitConstructor(declaration: IrConstructor, data: JsGenerationContext): JsStatement {
            assert(declaration.symbol.isPrimary)

            return context.translateFunction(declaration, irClass.name).makeStmt()
        }
    }

    private fun generateForInnerDeclaration(declaration: IrFunction): JsStatement {
        return declaration.accept(IrInnerDeclarationToJsTransformer(), context)
    }

    private fun generateClassMetadata(): JsStatement {


        val simpleName = irClass.symbol.name
        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }

        generateSuperClasses()

        val metadataRef = JsNameRef(Namer.METADATA, classNameRef)
        return JsExpressionStatement(jsAssignment(metadataRef, metadataLiteral))
    }

    private fun generateSuperClasses() {
        val superClasses = irClass.superClasses.filter { !it.isAny }

        val superList = mutableListOf<JsExpression>()

        for (superClass in superClasses) {
            superList += JsNameRef(superClass.owner.name.toJsName())
        }

        val inheritanceList = JsArrayLiteral(superList)

        val superPropertyInitializer = JsPropertyInitializer(JsNameRef(Namer.METADATA_SUPERTYPES), inheritanceList)
        metadataLiteral.propertyInitializers += superPropertyInitializer
    }

}
