package io.github.joke.caffeinate.mutable;

import com.palantir.javapoet.FieldSpec;
import io.github.joke.caffeinate.strategy.ClassModel;
import io.github.joke.caffeinate.strategy.GenerationStrategy;
import io.github.joke.caffeinate.strategy.Property;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableFieldStrategy implements GenerationStrategy {

    @Inject
    MutableFieldStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            FieldSpec field = FieldSpec.builder(property.getType(), property.getFieldName(), Modifier.PRIVATE)
                    .build();
            model.getFields().add(field);
        }
    }
}
