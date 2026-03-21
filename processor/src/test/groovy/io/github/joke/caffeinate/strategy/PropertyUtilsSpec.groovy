package io.github.joke.caffeinate.strategy

import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Name
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@Subject(PropertyUtils)
class PropertyUtilsSpec extends Specification {

    def 'isGetterMethod recognizes valid and invalid getter signatures'() {
        final method = mockMethod(name, returnKind, paramCount)

        expect:
        PropertyUtils.isGetterMethod(method) == expected

        where:
        name        | returnKind        | paramCount || expected
        'getName'   | TypeKind.DECLARED | 0          || true
        'isActive'  | TypeKind.BOOLEAN  | 0          || true
        'getName'   | TypeKind.DECLARED | 1          || false
        'getName'   | TypeKind.VOID     | 0          || false
        'get'       | TypeKind.DECLARED | 0          || false
        'is'        | TypeKind.BOOLEAN  | 0          || false
        'firstName' | TypeKind.DECLARED | 0          || false
    }

    def 'isSetterMethod recognizes valid and invalid setter signatures'() {
        final method = mockMethod(name, returnKind, paramCount)

        expect:
        PropertyUtils.isSetterMethod(method) == expected

        where:
        name      | returnKind        | paramCount || expected
        'setName' | TypeKind.VOID     | 1          || true
        'setName' | TypeKind.VOID     | 0          || false
        'setName' | TypeKind.DECLARED | 1          || false
        'set'     | TypeKind.VOID     | 1          || false
        'putName' | TypeKind.VOID     | 1          || false
    }

    def 'extractProperty derives field name from getter method name'() {
        final method = mockMethodWithAnnotations(methodName, returnKind, [])
        final property = PropertyUtils.extractProperty(method)

        expect:
        property.fieldName == expectedField
        property.getterName == methodName

        where:
        methodName     | returnKind        || expectedField
        'getFirstName' | TypeKind.DECLARED  || 'firstName'
        'isActive'     | TypeKind.BOOLEAN   || 'active'
    }

    def 'extractProperty throws for non-getter method name'() {
        final method = mockMethodWithAnnotations('firstName', TypeKind.DECLARED, [])

        when:
        PropertyUtils.extractProperty(method)

        then:
        thrown(IllegalArgumentException)
    }

    def 'setterNameForField produces correct setter name'() {
        expect:
        PropertyUtils.setterNameForField(fieldName) == expected

        where:
        fieldName   || expected
        'firstName' || 'setFirstName'
        'x'         || 'setX'
    }

    // --- helpers ---

    private ExecutableElement mockMethod(String name, TypeKind returnKind, int paramCount) {
        final method = Mock(ExecutableElement)
        final simpleName = Mock(Name)
        simpleName.toString() >> name
        method.simpleName >> simpleName
        method.parameters >> (0..<paramCount).collect { Mock(VariableElement) }

        final returnType = Mock(TypeMirror)
        returnType.kind >> returnKind
        method.returnType >> returnType

        return method
    }

    private ExecutableElement mockMethodWithAnnotations(String name, TypeKind returnKind, List annotationMirrors) {
        final method = Mock(ExecutableElement)
        final simpleName = Mock(Name)
        simpleName.toString() >> name
        method.simpleName >> simpleName
        method.parameters >> []

        final returnType = Mock(TypeMirror)
        returnType.kind >> returnKind
        method.returnType >> returnType
        method.annotationMirrors >> annotationMirrors

        return method
    }
}
