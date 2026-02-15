package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;

public class Property {

    private final String fieldName;
    private final TypeName type;
    private final String getterName;

    public Property(String fieldName, TypeName type, String getterName) {
        this.fieldName = fieldName;
        this.type = type;
        this.getterName = getterName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public TypeName getType() {
        return type;
    }

    public String getGetterName() {
        return getterName;
    }
}
