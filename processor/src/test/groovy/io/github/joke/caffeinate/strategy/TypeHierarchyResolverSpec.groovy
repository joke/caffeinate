package io.github.joke.caffeinate.strategy

import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

class TypeHierarchyResolverSpec extends Specification {

    Types types = Stub() {
        erasure(_) >> { TypeMirror t ->
            final erased = Stub(TypeMirror)
            erased.toString() >> "erased<${t.toString()}>"
            return erased
        }
    }

    @Subject(TypeHierarchyResolver)
    final resolver = new TypeHierarchyResolver(types)

    def 'returns abstract methods from a simple type element'() {
        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
        result[0] == method
    }

    def 'returns empty list when no abstract methods'() {
        final method = mockConcreteMethod('toString')
        final element = mockTypeElement([], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.empty
    }

    def 'stops traversal when superclass type kind is NONE'() {
        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
    }

    def 'stops traversal when superclass type kind is ERROR'() {
        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([], TypeKind.ERROR, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
    }

    def 'skips non-abstract superclass'() {
        final superElement = Stub(TypeElement)
        superElement.interfaces >> []
        final superSuperclass = Stub(TypeMirror)
        superSuperclass.kind >> TypeKind.NONE
        superElement.superclass >> superSuperclass
        superElement.enclosedElements >> []
        superElement.modifiers >> EnumSet.noneOf(Modifier)
        superElement.qualifiedName >> mockName('test.Parent')

        final superMirror = Stub(TypeMirror)
        superMirror.kind >> TypeKind.DECLARED
        types.asElement(superMirror) >> superElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElementWithSuperclass([], superMirror, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
        result[0] == method
    }

    def 'skips java.lang.Object superclass'() {
        final superElement = Stub(TypeElement)
        superElement.qualifiedName >> mockName('java.lang.Object')
        superElement.modifiers >> EnumSet.of(Modifier.ABSTRACT)

        final superMirror = Stub(TypeMirror)
        superMirror.kind >> TypeKind.DECLARED
        types.asElement(superMirror) >> superElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElementWithSuperclass([], superMirror, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
        result[0] == method
    }

    def 'collects methods from interface hierarchy'() {
        final ifaceMethod = mockAbstractMethod('getId')
        final ifaceElement = mockTypeElement([], TypeKind.NONE, [ifaceMethod])

        final ifaceMirror = Stub(TypeMirror)
        types.asElement(ifaceMirror) >> ifaceElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([ifaceMirror], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 2
    }

    def 'skips interface element that is not a TypeElement'() {
        final nonTypeElement = Stub(Element)

        final ifaceMirror = Stub(TypeMirror)
        types.asElement(ifaceMirror) >> nonTypeElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([ifaceMirror], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
        result[0] == method
    }

    def 'deduplicates methods with same key'() {
        final ifaceMethod = mockAbstractMethod('getName')
        final ifaceElement = mockTypeElement([], TypeKind.NONE, [ifaceMethod])

        final ifaceMirror = Stub(TypeMirror)
        types.asElement(ifaceMirror) >> ifaceElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElement([ifaceMirror], TypeKind.NONE, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 1
    }

    def 'collects methods from abstract superclass hierarchy'() {
        final superMethod = mockAbstractMethod('getId')
        final superElement = mockTypeElement([], TypeKind.NONE, [superMethod])
        superElement.modifiers >> EnumSet.of(Modifier.ABSTRACT)
        superElement.qualifiedName >> mockName('test.Parent')

        final superMirror = Stub(TypeMirror)
        superMirror.kind >> TypeKind.DECLARED
        types.asElement(superMirror) >> superElement

        final method = mockAbstractMethod('getName')
        final element = mockTypeElementWithSuperclass([], superMirror, [method])

        when:
        final result = resolver.getAllAbstractMethods(element)

        then:
        0 * _

        expect:
        result.size() == 2
    }

    // --- helpers ---

    private ExecutableElement mockAbstractMethod(String name) {
        final method = Stub(ExecutableElement)
        final simpleName = mockName(name)
        method.simpleName >> simpleName
        method.modifiers >> EnumSet.of(Modifier.ABSTRACT, Modifier.PUBLIC)
        method.parameters >> []
        method.kind >> ElementKind.METHOD

        return method
    }

    private ExecutableElement mockConcreteMethod(String name) {
        final method = Stub(ExecutableElement)
        final simpleName = mockName(name)
        method.simpleName >> simpleName
        method.modifiers >> EnumSet.of(Modifier.PUBLIC)
        method.kind >> ElementKind.METHOD
        return method
    }

    private TypeElement mockTypeElement(List<TypeMirror> interfaces, TypeKind superKind, List<Element> enclosed) {
        final element = Stub(TypeElement)
        element.interfaces >> interfaces

        final superclass = Stub(TypeMirror)
        superclass.kind >> superKind
        element.superclass >> superclass

        element.enclosedElements >> enclosed
        element.modifiers >> EnumSet.of(Modifier.ABSTRACT)
        element.qualifiedName >> mockName('test.Element')

        return element
    }

    private TypeElement mockTypeElementWithSuperclass(List<TypeMirror> interfaces, TypeMirror superMirror, List<Element> enclosed) {
        final element = Stub(TypeElement)
        element.interfaces >> interfaces
        element.superclass >> superMirror
        element.enclosedElements >> enclosed
        element.modifiers >> EnumSet.of(Modifier.ABSTRACT)
        element.qualifiedName >> mockName('test.Element')

        return element
    }

    private Name mockName(String value) {
        final name = Stub(Name)
        name.toString() >> value
        name.contentEquals(_) >> { CharSequence s -> value == s.toString() }
        return name
    }
}
