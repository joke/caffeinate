package io.github.joke.caffeinate.strategy;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

public class ClassModel {

    private String className = "";
    private boolean hasErrors = false;
    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<TypeName> superinterfaces = new ArrayList<>();
    private final List<Property> properties = new ArrayList<>();
    private final List<FieldSpec> fields = new ArrayList<>();
    private final List<MethodSpec> methods = new ArrayList<>();
    private final List<ExecutableElement> declaredSetters = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public List<TypeName> getSuperinterfaces() {
        return superinterfaces;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<FieldSpec> getFields() {
        return fields;
    }

    public List<MethodSpec> getMethods() {
        return methods;
    }

    public List<ExecutableElement> getDeclaredSetters() {
        return declaredSetters;
    }
}
