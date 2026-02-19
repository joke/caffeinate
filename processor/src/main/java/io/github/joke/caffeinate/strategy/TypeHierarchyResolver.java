package io.github.joke.caffeinate.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

public class TypeHierarchyResolver {

    private final Types types;

    @Inject
    TypeHierarchyResolver(Types types) {
        this.types = types;
    }

    public List<ExecutableElement> getAllAbstractMethods(TypeElement element) {
        List<ExecutableElement> result = new ArrayList<>();
        collectAbstractMethods(element, result, new HashSet<>());
        return result;
    }

    private void collectAbstractMethods(TypeElement element, List<ExecutableElement> result, Set<String> seen) {
        for (TypeMirror iface : element.getInterfaces()) {
            Element ifaceElement = types.asElement(iface);
            if (ifaceElement instanceof TypeElement) {
                collectAbstractMethods((TypeElement) ifaceElement, result, seen);
            }
        }

        TypeMirror superclass = element.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && superclass.getKind() != TypeKind.ERROR) {
            TypeElement superElement = (TypeElement) types.asElement(superclass);
            if (superElement != null
                    && !superElement.getQualifiedName().contentEquals("java.lang.Object")
                    && superElement.getModifiers().contains(Modifier.ABSTRACT)) {
                collectAbstractMethods(superElement, result, seen);
            }
        }

        for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                if (seen.add(methodKey(method))) {
                    result.add(method);
                }
            }
        }
    }

    private String methodKey(ExecutableElement method) {
        StringBuilder key = new StringBuilder(method.getSimpleName().toString()).append("::");
        method.getParameters()
                .forEach(p -> key.append(types.erasure(p.asType())).append(","));
        return key.toString();
    }
}
