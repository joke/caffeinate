package io.github.joke.objects.mutable;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.Property;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MutableConstructorStrategy implements GenerationStrategy {

    @Inject
    MutableConstructorStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        MethodSpec noArgs =
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .build();
        model.getMethods().add(noArgs);

        if (!model.getProperties().isEmpty()) {
            MethodSpec.Builder allArgs =
                    MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            for (Property property : model.getProperties()) {
                allArgs.addParameter(
                        ParameterSpec.builder(
                                        property.getType(),
                                        property.getFieldName())
                                .build());
                allArgs.addStatement(
                        "this.$N = $N",
                        property.getFieldName(),
                        property.getFieldName());
            }

            model.getMethods().add(allArgs.build());
        }
    }
}
