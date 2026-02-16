package io.github.joke.objects.strategy;

import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class PropertyDiscoveryStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    PropertyDiscoveryStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement method :
                ElementFilter.methodsIn(source.getEnclosedElements())) {
            if (PropertyUtils.isGetterMethod(method)) {
                model.getProperties().add(PropertyUtils.extractProperty(method));
            } else {
                reportError(method, model);
            }
        }
    }

    private void reportError(ExecutableElement method, ClassModel model) {
        if (!method.getParameters().isEmpty()) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must have no parameters",
                    method);
        } else if (method.getReturnType().getKind()
                == javax.lang.model.type.TypeKind.VOID) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must not return void",
                    method);
        } else {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Methods in @Immutable interfaces must follow"
                            + " get*/is* naming convention",
                    method);
        }
        model.setHasErrors(true);
    }
}
