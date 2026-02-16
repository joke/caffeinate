package io.github.joke.caffeinate.strategy;

import com.palantir.javapoet.MethodSpec;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class GetterStrategy implements GenerationStrategy {

    @Inject
    GetterStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        for (Property property : model.getProperties()) {
            MethodSpec getter = MethodSpec.methodBuilder(property.getGetterName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(property.getType())
                    .addStatement("return this.$N", property.getFieldName())
                    .build();
            model.getMethods().add(getter);
        }
    }
}
