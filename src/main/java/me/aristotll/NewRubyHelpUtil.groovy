package me.aristotll

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.ruby.ruby.codeInsight.references.RPsiPolyVariantReference
import org.jetbrains.plugins.ruby.ruby.codeInsight.references.SymbolResolveResult
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.RMethodSymbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.TypedSymbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.Context
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RSymbolType
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeUtil
import org.jetbrains.plugins.ruby.ruby.lang.documentation.RubyHelpUtil
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RConstant
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RClassVariable
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RInstanceVariable

@CompileStatic
class NewRubyHelpUtil {
    public static final String MULTIPLE_SYMBOLS_FOUND = '\0'

    static String getDocOfElement(@NotNull final RPsiElement targetElement) {
        StringUtil.notNullize(getDocOfElementInner(targetElement))

    }

    @Nullable
    private static String getDocOfElementInner(@NotNull final RPsiElement targetElement) {
        final String doc = createDocUsingResolveToSymbol(targetElement)
        if (doc != null) {
            return doc
        }
        if (targetElement instanceof RContainer) {
            final RContainer container = (RContainer) targetElement
            final Symbol containerSymbol = SymbolUtil.getSymbolByContainer(container)
            if (containerSymbol != null) {
                return createDocForSymbol(containerSymbol, targetElement)
            }
        }
        return null
    }


    @Nullable
    static String createTypeText(final RType type) {
        if (type instanceof RSymbolType) {
            final Symbol symbol = ((RSymbolType) type).symbol
            return symbol.FQNWithNesting.fullPath
        }
        return type.presentableName
    }

    @Nullable
    @CompileDynamic
    static RType getInferredMethodType(final RMethod method) {
        RubyHelpUtil.getInferredMethodType(method)
    }


    @Nullable
    static String createDocUsingResolveToSymbol(final RPsiElement elementToFinderReference) {
        final PsiReference ref = elementToFinderReference.containingFile
                .findReferenceAt(elementToFinderReference.textOffset)
        if (!(ref instanceof RPsiPolyVariantReference)) {
            return null
        }
        final ResolveResult[] results = ((RPsiPolyVariantReference) ref).multiResolve(false)
        if (ArrayUtil.isEmpty(results)) {
            return null
        }
        final ArrayList<PsiElement> resolveElements = new ArrayList<PsiElement>()
        Symbol resolvedSymbol = null
        String docForSymbol = null
        for (final ResolveResult result : results) {
            if (result instanceof SymbolResolveResult) {
                final Symbol symbol = ((SymbolResolveResult) result).symbol
                if (symbol != null) {
                    if (docForSymbol == null) {
                        docForSymbol = createDocForSymbol(symbol, elementToFinderReference)
                        resolvedSymbol = symbol
                    } else if (resolvedSymbol != symbol) {
                        docForSymbol = MULTIPLE_SYMBOLS_FOUND
                    }
                }
            }
            final PsiElement element = result.element
            if (element != null) {
                resolveElements.add(element)
            }
        }
        if (docForSymbol != null && docForSymbol != MULTIPLE_SYMBOLS_FOUND) {
            return docForSymbol
        }
        return null
    }


    @Nullable
    static String createDocForSymbol(@NotNull final Symbol targetSymbol,
                                     @NotNull final RPsiElement targetElement) {
        appendTypeInfo(targetElement, targetSymbol)
//        appendTypeInfoOrigin(targetElement, targetSymbol)
    }

    static String appendTypeInfoOrigin(RPsiElement targetElement, Symbol targetSymbol) {
        RType type = null
        if (isVariableLike(targetElement)) {
            type = RTypeUtil.getExpressionType(targetElement)
        }
        if (type == null) {
            if (targetElement instanceof RMethod) {
                type = getInferredMethodType((RMethod) targetElement)
            }
            if (type == null && targetElement == null && isVariableLike(targetElement)) {
                type = RTypeUtil.getExpressionType(targetElement)
            }
        }
        if (type == null && targetSymbol instanceof TypedSymbol) {
            Context context = (targetElement != null) ? Context.getContext(targetElement) : null
            context = ((context == null && targetElement != null)
                    ? Context.getContext(targetElement)
                    : context)
            type = ((context != null) ? ((TypedSymbol) targetSymbol).getType(context) : null)
        }
        final String typeName
        if (RTypeUtil.isNullOrEmpty(type)) {
            typeName = 'Object'
        } else {
            typeName = createTypeText(type)
        }
        if (targetSymbol instanceof RMethodSymbol) {
            return "# @return [$typeName] " // true doer
        } else {

            return "# @param [$typeName] " // true doer
        }
    }

    private static String appendTypeInfo(@NotNull final RPsiElement targetElement,
                                         @NotNull final Symbol targetSymbol) {
        def variableLike = isVariableLike(targetElement)
        RType type = variableLike ? RTypeUtil.getExpressionType(targetElement) : null
        if (type == null && targetElement instanceof RMethod) {
            type = getInferredMethodType((RMethod) targetElement)
        }
        if (type == null && targetSymbol instanceof TypedSymbol) {
            Context context = Context.getContext(targetElement)
            context = context == null ? Context.getContext(targetElement) : context
            type = context == null ? null : ((TypedSymbol) targetSymbol).getType(context)
        }
        final String typeName = RTypeUtil.isNullOrEmpty(type) ?
                'Object' : createTypeText(type)
        return "# @${targetSymbol instanceof RMethodSymbol ? 'return' : 'param'} [$typeName] "
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    @Contract(value = "null -> false", pure = true)
    static boolean isVariableLike(@Nullable final RPsiElement element) {
        element instanceof RIdentifier ||
                element instanceof RInstanceVariable || element instanceof RClassVariable || element instanceof RConstant
    }

}