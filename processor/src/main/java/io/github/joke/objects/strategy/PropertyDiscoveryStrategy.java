package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
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
            if (!method.getParameters().isEmpty()) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must have no parameters",
                        method);
                model.setHasErrors(true);
                continue;
            }
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must not return void",
                        method);
                model.setHasErrors(true);
                continue;
            }

            String methodName = method.getSimpleName().toString();
            String fieldName;

            if (methodName.startsWith("get") && methodName.length() > 3) {
                fieldName =
                        Character.toLowerCase(methodName.charAt(3))
                                + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                fieldName =
                        Character.toLowerCase(methodName.charAt(2))
                                + methodName.substring(3);
            } else {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Methods in @Immutable interfaces must follow"
                                + " get*/is* naming convention",
                        method);
                model.setHasErrors(true);
                continue;
            }

            TypeName type = TypeName.get(method.getReturnType());
            model.getProperties().add(new Property(fieldName, type, methodName));
        }
    }
}
