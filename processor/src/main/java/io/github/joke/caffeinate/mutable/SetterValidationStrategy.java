package io.github.joke.caffeinate.mutable;

import com.palantir.javapoet.TypeName;
import io.github.joke.caffeinate.strategy.ClassModel;
import io.github.joke.caffeinate.strategy.GenerationStrategy;
import io.github.joke.caffeinate.strategy.Property;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class SetterValidationStrategy implements GenerationStrategy {

    private final Messager messager;

    @Inject
    SetterValidationStrategy(Messager messager) {
        this.messager = messager;
    }

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (ExecutableElement setter : model.getDeclaredSetters()) {
            String setterName = setter.getSimpleName().toString();
            String expectedField = Character.toLowerCase(setterName.charAt(3)) + setterName.substring(4);
            TypeName paramType = TypeName.get(setter.getParameters().get(0).asType());

            boolean matched = false;
            for (Property property : model.getProperties()) {
                if (property.getFieldName().equals(expectedField)
                        && property.getType().equals(paramType)) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Setter " + setterName + " does not match any getter-derived property",
                        setter);
                model.setHasErrors(true);
            }
        }
    }
}
