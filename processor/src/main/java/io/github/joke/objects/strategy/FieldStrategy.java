package io.github.joke.objects.strategy;

import com.palantir.javapoet.FieldSpec;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class FieldStrategy implements GenerationStrategy {

    @Inject
    FieldStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            FieldSpec field =
                    FieldSpec.builder(
                                    property.getType(),
                                    property.getFieldName(),
                                    Modifier.PRIVATE,
                                    Modifier.FINAL)
                            .build();
            model.getFields().add(field);
        }
    }
}
