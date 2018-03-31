package me.aristotll

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
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
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.*
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.impl.REmptyType
import org.jetbrains.plugins.ruby.ruby.lang.documentation.RubyHelpUtil
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RAliasStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RBlockCall
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RConstant
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RGlobalVariable
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RClassVariable
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RField
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RInstanceVariable

@CompileStatic
class NewRubyHelpUtil {
    public static final String MULTIPLE_SYMBOLS_FOUND = '\0'

    @Nullable
    static String getSimpleInfo(@Nullable final RPsiElement targetElement) {
        if (targetElement == null || targetElement instanceof PsiFile) {
            return null
        }
        return appendTypeInfo(null, targetElement, null)
    }

    @Nullable
    static String getHelpByElement(@Nullable final RPsiElement originalRPsiElement,
                                   @NotNull final RPsiElement targetElement) {
        if (targetElement instanceof RFile) {
            return null
        }
        if (originalRPsiElement != null) {
            final String help = createHelpUsingResolveToSymbol(originalRPsiElement, originalRPsiElement, targetElement)
            if (help != null) {
                return help
            }
        }
        if (targetElement instanceof RContainer) {
            final RContainer container = (RContainer) targetElement
            final Symbol containerSymbol = SymbolUtil.getSymbolByContainer(container)
            if (containerSymbol != null) {
                return createHelpForSymbol(containerSymbol, originalRPsiElement, targetElement)
            }
            return null
        } else {
            if (!wantedCriteria(targetElement)) {
                return null
            }
            return createHelpUsingResolveToSymbol(targetElement, originalRPsiElement, targetElement)
        }
    }

    private static boolean wantedCriteria(RPsiElement targetElement) {
        isVariableLike(targetElement) || targetElement instanceof RConstant ||
                targetElement instanceof RField ||
                targetElement instanceof RGlobalVariable ||
                targetElement instanceof RAliasStatement ||
                targetElement instanceof RCall || targetElement instanceof RBlockCall
    }

    @Nullable
    static String createTypeText(final RType type) {
        if (type instanceof RSymbolType) {
            final Symbol symbol = ((RSymbolType) type).symbol
            final StringBuilder typeName = new StringBuilder()
            final String name = symbol.FQNWithNesting.fullPath
            typeName.append(name)
            return typeName.toString()
        }
        return type.presentableName
    }

    @Nullable
    @CompileDynamic
    static RType getInferredMethodType(final RMethod method) {
        RubyHelpUtil.getInferredMethodType(method)
    }


    @Nullable
    static String createHelpUsingResolveToSymbol(final RPsiElement elementToFinderReference,
                                                 final RPsiElement originalRPsiElement,
                                                 final RPsiElement targetElement) {
        final PsiReference ref = elementToFinderReference.containingFile
                .findReferenceAt(elementToFinderReference.textOffset)
        if (!(ref instanceof RPsiPolyVariantReference)) {
            return null
        }
        final ResolveResult[] results = ((RPsiPolyVariantReference) ref).multiResolve(false)
        if (results.length == 0) {
            return null
        }
        final ArrayList<PsiElement> resolveElements = new ArrayList<PsiElement>()
        Symbol resolvedSymbol = null
        String helpForSymbol = null
        for (final ResolveResult result : results) {
            if (result instanceof SymbolResolveResult) {
                final Symbol symbol = ((SymbolResolveResult) result).symbol
                if (symbol != null) {
                    if (helpForSymbol == null) {
                        helpForSymbol = createHelpForSymbol(symbol, originalRPsiElement, targetElement)
                        resolvedSymbol = symbol
                    } else if (resolvedSymbol != symbol) {
                        helpForSymbol = MULTIPLE_SYMBOLS_FOUND
                    }
                }
            }
            final PsiElement element = result.element
            if (element != null) {
                resolveElements.add(element)
            }
        }
        if (helpForSymbol != null && helpForSymbol != MULTIPLE_SYMBOLS_FOUND) {
            return helpForSymbol
        }
        return null
    }


    @Nullable
    static String createHelpForSymbol(@NotNull final Symbol targetSymbol,
                                      @Nullable final RPsiElement originalRPsiElement,
                                      @NotNull final RPsiElement targetElement) {
        final StringBuilder builder = new StringBuilder()
        builder.append appendTypeInfo(originalRPsiElement, targetElement, targetSymbol)
        builder.length() > 0 ? builder.toString() : null
    }


    static def appendTypeInfo(@Nullable final RPsiElement originalRPsiElement,
                              final RPsiElement targetElement,
                              @Nullable final Symbol targetSymbol) {
        RType type = null
        if (isVariableLike(originalRPsiElement)) {
            type = RTypeUtil.getExpressionType(originalRPsiElement)
        }
        if (type == null) {
            if (targetElement instanceof RMethod) {
                type = getInferredMethodType((RMethod) targetElement)
            }
            if (type == null && originalRPsiElement == null && isVariableLike(targetElement)) {
                type = RTypeUtil.getExpressionType(targetElement)
            }
        }
        if (type == null && targetSymbol instanceof TypedSymbol) {
            Context context = (originalRPsiElement != null) ? Context.getContext(originalRPsiElement) : null
            context = ((context == null && targetElement != null)
                    ? Context.getContext(targetElement)
                    : context)
            type = ((context != null) ? ((TypedSymbol) targetSymbol).getType(context) : null)
        }
        final String typeName
        if (type != null && type != REmptyType.INSTANCE) {
            typeName = createTypeText(type)
        } else {
            typeName = 'Object'
        }
        if (targetSymbol instanceof RMethodSymbol) {
            return "# @return [$typeName] " // true doer
        } else {
            return "# @param [$typeName] " // true doer
        }
    }

    @Contract(value = "null -> false", pure = true)
    static boolean isVariableLike(@Nullable final RPsiElement element) {
        element instanceof RIdentifier ||
                element instanceof RInstanceVariable || element instanceof RClassVariable || element instanceof RConstant
    }

}