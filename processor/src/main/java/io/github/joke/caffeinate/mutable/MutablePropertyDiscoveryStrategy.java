package io.github.joke.caffeinate.mutable;

import io.github.joke.caffeinate.strategy.ClassModel;
import io.github.joke.caffeinate.strategy.GenerationStrategy;
import io.github.joke.caffeinate.strategy.PropertyUtils;
import io.github.joke.caffeinate.strategy.TypeHierarchyResolver;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

public class MutablePropertyDiscoveryStrategy implements GenerationStrategy {

    private final Messager messager;
    private final TypeHierarchyResolver resolver;

    @Inject
    MutablePropertyDiscoveryStrategy(Messager messager, TypeHierarchyResolver resolver) {
        this.messager = messager;
        this.resolver = resolver;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement method : resolver.getAllAbstractMethods(source)) {
            if (PropertyUtils.isGetterMethod(method)) {
                model.getProperties().add(PropertyUtils.extractProperty(method));
            } else if (PropertyUtils.isSetterMethod(method)) {
                model.getDeclaredSetters().add(method);
            } else {
                reportError(method, model);
            }
        }
    }

    private void reportError(ExecutableElement method, ClassModel model) {
        if (!method.getParameters().isEmpty()) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Mutable interfaces must have no parameters (except setters)",
                    method);
        } else if (method.getReturnType().getKind() == TypeKind.VOID) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Void methods in @Mutable interfaces must follow set* naming convention",
                    method);
        } else {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Mutable interfaces must follow get*/is*/set* naming convention",
                    method);
        }
        model.setHasErrors(true);
    }
}
