/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.lang.type.LuaType
import com.tang.intellij.lua.lang.type.LuaTypeSet
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.LuaNameStub

/**

 * Created by TangZX on 2017/4/12.
 */
open class LuaNameExpressionImpl : StubBasedPsiElementBase<LuaNameStub>, LuaExpression, LuaGlobalVar {
    internal constructor(stub: LuaNameStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    internal constructor(node: ASTNode) : super(node)

    internal constructor(stub: LuaNameStub, nodeType: IElementType, node: ASTNode) : super(stub, nodeType, node) {}

    override fun getReference(): PsiReference? {
        val references = references

        if (references.isNotEmpty())
            return references[0]
        return null
    }

    override fun guessType(context: SearchContext): LuaTypeSet? {
        return RecursionManager.doPreventingRecursion(this, true) {
            var typeSet = LuaTypeSet.create()
            val nameExpr = this as LuaNameExpr

            val multiResolve = LuaPsiResolveUtil.multiResolve(nameExpr, context)
            if (multiResolve.isEmpty()) {
                typeSet.addType(LuaType.createGlobalType(nameExpr))
            } else {
                multiResolve.forEach {
                    val set = getTypeSet(context, it)
                    typeSet = typeSet.union(set)
                }
            }
            typeSet
        }
    }

    private fun getTypeSet(context: SearchContext, def: PsiElement): LuaTypeSet? {
        when (def) {
            is LuaNameExpr -> {
                var typeSet: LuaTypeSet? = null
                val luaAssignStat = PsiTreeUtil.getParentOfType(def, LuaAssignStat::class.java)
                if (luaAssignStat != null) {
                    val comment = luaAssignStat.comment
                    //优先从 Comment 猜
                    typeSet = comment?.guessType(context)
                    //再从赋值猜
                    if (typeSet == null) {
                        val exprList = luaAssignStat.valueExprList
                        if (exprList != null)
                            typeSet = exprList.guessTypeAt(0, context)//TODO : multi
                    }
                }
                //Global
                if (LuaPsiResolveUtil.resolveLocal(def, context) == null) {
                    if (typeSet == null)
                        typeSet = LuaTypeSet.create()
                    typeSet.addType(LuaType.createGlobalType(def))
                }
                return typeSet
            }
            is LuaTypeGuessable -> return def.guessType(context)
            else -> return null
        }
    }

    override fun getNameRef(): LuaNameExpr? = this as LuaNameExpr
}
