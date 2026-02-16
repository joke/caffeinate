package io.github.joke.objects.mutable;

import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.PropertyUtils;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class MutablePropertyDiscoveryStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    MutablePropertyDiscoveryStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement method :
                ElementFilter.methodsIn(source.getEnclosedElements())) {
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
                    "Methods in @Mutable interfaces must have no parameters"
                            + " (except setters)",
                    method);
        } else if (method.getReturnType().getKind() == TypeKind.VOID) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Void methods in @Mutable interfaces"
                            + " must follow set* naming convention",
                    method);
        } else {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Mutable interfaces must follow"
                            + " get*/is*/set* naming convention",
                    method);
        }
        model.setHasErrors(true);
    }
}
