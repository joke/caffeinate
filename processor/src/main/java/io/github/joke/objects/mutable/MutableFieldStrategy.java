package io.github.joke.objects.mutable;

import com.palantir.javapoet.FieldSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableFieldStrategy implements GenerationStrategy {

    @Inject
    MutableFieldStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            FieldSpec field =
                    FieldSpec.builder(
                                    property.getType(),
                                    property.getFieldName(),
                                    Modifier.PRIVATE)
                            .build();
            model.getFields().add(field);
        }
    }
}
