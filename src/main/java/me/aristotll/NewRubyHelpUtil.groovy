package me.aristotll

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.ruby.RBundle
import org.jetbrains.plugins.ruby.motion.RubyMotionUtil
import org.jetbrains.plugins.ruby.ruby.codeInsight.references.RPsiPolyVariantReference
import org.jetbrains.plugins.ruby.ruby.codeInsight.references.SymbolResolveResult
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.RMethodSymbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.RMethodSymbolImpl
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.TypedSymbol
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.*
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.impl.REmptyType
import org.jetbrains.plugins.ruby.ruby.lang.RubyElementNameAndDescriptionProvider
import org.jetbrains.plugins.ruby.ruby.lang.documentation.RubyCommentsUtil
import org.jetbrains.plugins.ruby.ruby.lang.documentation.RubyDocumentationMarkupUtil
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtil
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
        def s = appendTypeInfo(null, targetElement, null)
        return s
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
            if (!isVariableLike(targetElement) && !(targetElement instanceof RConstant)
                    && !(targetElement instanceof RField) && !(targetElement instanceof RGlobalVariable)
                    && !(targetElement instanceof RAliasStatement) && !(targetElement instanceof RCall)
                    && !(targetElement instanceof RBlockCall)) {
                return null
            }
            final String help = createHelpUsingResolveToSymbol(targetElement, originalRPsiElement, targetElement)
            if (help != null) {
                return help
            }
            return getDescriptionWithoutSymbol(targetElement)
        }
    }
// rtype  Ruby type
    static def appendElementTypeFullInfo(@NotNull final RType type) {
        final String typeName = createTypeText(type)
        return "# @param [$typeName] "
    }

    @Nullable
    static String createTypeText(final RType type) {
        if (type instanceof RSymbolType) {
            final Symbol symbol = ((RSymbolType) type).getSymbol()
            final StringBuilder typeName = new StringBuilder()
            final String name = symbol.getFQNWithNesting().getFullPath()
            typeName.append(name)
            return typeName.toString()
        }
        return type.getPresentableName()
    }

    @CompileDynamic
    @Nullable
    static RType getInferredMethodType(final RMethod method) {
        return TypeInferenceHelper.inferTypeOfMethodCall(method, Collections.emptyList())
    }

    @CompileDynamic
    @NotNull
    static String getPresentableName(@NotNull final PsiElement element) {
        if (element instanceof RContainer) {
            final String name = ((RContainer) element).getName()
            if (element instanceof RMethod) {
                return ((RMethod) element).getPresentableName(true)
            } else {
                return name
            }
        } else {
            final RubyElementNameAndDescriptionProvider[] nameAndDescriptionProviders = Extensions
                    .getExtensions(RubyElementNameAndDescriptionProvider.EXTENSION_POINT_NAME)
            final int length = nameAndDescriptionProviders.length
            int i = 0
            while (i < length) {
                final RubyElementNameAndDescriptionProvider provider = nameAndDescriptionProviders[i]
                final String variant = provider.getPresentableName(element)
                if (variant != null) {
                    return variant
                } else {
                    ++i
                }
            }
            return element.getText()
        }
    }

    static void appendPresentableNameForAlias(@Nullable final String newName, @Nullable final String oldName,
                                              final StringBuilder builder) {
        final String name = StringUtil.isEmpty(newName) ? "[undefined]" : newName
        builder.append(name)
        final String originalName = StringUtil.isEmpty(oldName) ? "[undefined]" : oldName
        builder.append("(").append(RBundle.message("ruby.doc.original")).append(": ")
        builder.append(originalName)
        builder.append(')')
    }

    static void appendDocumentation(final StringBuilder builder, final PsiElement targetElement,
                                    @Nullable final Symbol targetSymbol) {
        String descriptionText = RubyCommentsUtil.getPsiHelp(targetElement)
        if (descriptionText == null && targetSymbol != null && targetSymbol.getType() == Type.ALIAS) {
            final Symbol sourceSymbol = SymbolUtil.getMethodSymbolByAlias(targetSymbol)
            if (sourceSymbol != null) {
                final PsiElement latestDeclaration = sourceSymbol.getPsiElement()
                if (latestDeclaration != null) {
                    descriptionText = RubyCommentsUtil.getPsiHelp(latestDeclaration)
                }
            }
        }
        if (descriptionText != null) {
            descriptionText = RubyDocumentationMarkupUtil.processCodeDocumentation(targetSymbol, descriptionText)
        }
        final RubyMotionUtil motionUtil = RubyMotionUtil.getInstance()
        if (descriptionText == null && motionUtil.isMotionSymbol(targetSymbol)) {
            descriptionText = motionUtil.getMotionDoc(targetElement, targetSymbol)
        }
        if (builder.length() > 0) {
            builder.append("<br>").append("<hr>")
        }
        if (descriptionText != null) {
            builder.append(motionUtil.isMotionSymbol(targetSymbol) ? "" : getDocumentationHeader())
            builder.append(descriptionText)
        } else {
            builder.append(RBundle.message("ruby.doc.not.found"))
        }
    }

    static String getDocumentationHeader() {
        return "<h2>" + RBundle.message("ruby.doc.documentation") + ":</h2>"
    }

    @Nullable
    static String createHelpUsingResolveToSymbol(final RPsiElement elementToFinderReference,
                                                 final RPsiElement originalRPsiElement,
                                                 final RPsiElement targetElement) {
        final PsiReference ref = elementToFinderReference.getContainingFile()
                .findReferenceAt(elementToFinderReference.getTextOffset())
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
                final Symbol symbol = ((SymbolResolveResult) result).getSymbol()
                if (symbol != null) {
                    if (helpForSymbol == null) {
                        helpForSymbol = createHelpForSymbol(symbol, originalRPsiElement, targetElement)
                        resolvedSymbol = symbol
                    } else if (resolvedSymbol != symbol) {
                        helpForSymbol = MULTIPLE_SYMBOLS_FOUND
                    }
                }
            }
            final PsiElement element = result.getElement()
            if (element != null) {
                resolveElements.add(element)
            }
        }
        if (helpForSymbol != null && helpForSymbol != MULTIPLE_SYMBOLS_FOUND) {
            return helpForSymbol
        }
        return getDescriptionWithoutSymbol((RPsiElement) resolveElements.get(0))
    }

    @Nullable
    static String getDescriptionWithoutSymbol(@NotNull final RPsiElement targetElement) {
        final StringBuilder builder = new StringBuilder()
        final String shortDescription = getSimpleInfo(targetElement)
        if (shortDescription != null) {
            builder.append(shortDescription)
        }
        appendDocumentation(builder, targetElement, null)
        return (builder.length() > 0) ? builder.toString() : null
    }

    @Nullable
    static String createHelpForSymbol(@NotNull final Symbol targetSymbol,
                                      @Nullable final RPsiElement originalRPsiElement,
                                      @NotNull final RPsiElement targetElement) {
        final StringBuilder builder = new StringBuilder()
//        appendSymbolKindInfo(targetSymbol, builder, true);
//        appendVisibilityAndLocation(targetElement, targetSymbol, builder, true); // location
        builder.append appendTypeInfo(originalRPsiElement, targetElement, targetSymbol)
/*        if (!( targetSymbol instanceof RMethod )) {

            appendPresentableName(targetElement, targetSymbol, builder)
        }*/
//        appendSuperClassInfo(targetSymbol, builder, true, (PsiElement) originalRPsiElement);//
//        appendPartialDeclarations(targetSymbol, targetElement, builder);
//        appendDocumentation(builder, targetElement, targetSymbol)
        return (builder.length() > 0) ? builder.toString() : null
    }

    static void appendPresentableName(@NotNull final PsiElement targetElement,
                                      @Nullable final Symbol targetSymbol, final StringBuilder builder) {
        if (targetSymbol != null && targetSymbol.getType() == Type.ALIAS) {
            if (targetElement instanceof RCall) {
                final List<RPsiElement> args = ((RCall) targetElement).getArguments()
                if (args.size() == 2) {
                    final RPsiElement newName = args.get(0)
                    final RPsiElement oldName = args.get(1)
                    appendPresentableNameForAlias(
                            (newName != null) ? RubyPsiUtil.getElementText(newName) : null,
                            (oldName != null) ? RubyPsiUtil.getElementText(oldName) : null, builder
                    )
                    return
                }
            } else if (targetElement instanceof RAliasStatement) {
                final RAliasStatement alias = (RAliasStatement) targetElement
                appendPresentableNameForAlias(alias.getName(), alias.getOldName(), builder)
                return
            }
        }
        final String presentableName = getPresentableName(targetElement)
        builder.append(
                presentableName)
    }

    static def appendTypeInfo(@Nullable final RPsiElement originalRPsiElement, final RPsiElement targetElement,
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
        return element instanceof RIdentifier || element instanceof RInstanceVariable || element instanceof RClassVariable || element instanceof RConstant
    }

}