package io.github.joke.caffeinate.mutable

import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.TypeHierarchyResolver
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.processing.Messager
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@Subject(MutablePropertyDiscoveryStrategy)
class MutablePropertyDiscoveryStrategySpec extends Specification {

    Messager messager = Mock()
    Types types = Mock()
    final resolver = new TypeHierarchyResolver(types)
    final strategy = new MutablePropertyDiscoveryStrategy(messager, resolver)

    def 'adds getter methods to properties'() {
        final method = mockGetterMethod('getName', TypeKind.DECLARED)
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * messager._

        expect:
        model.properties.size() == 1
        model.properties[0].fieldName == 'name'
    }

    def 'adds setter methods to declared setters'() {
        final method = mockSetterMethod('setName')
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * messager._

        expect:
        model.declaredSetters.size() == 1
        model.declaredSetters[0] == method
    }

    def 'reports error for method with parameters that is not a setter'() {
        final param1 = Mock(VariableElement)
        final param2 = Mock(VariableElement)
        final method = mockExecutableElement('doSomething', TypeKind.VOID, [param1, param2])
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('must have no parameters') }, method)
        0 * messager._

        expect:
        model.hasErrors()
    }

    def 'reports error for void method without set* name'() {
        final method = mockExecutableElement('doSomething', TypeKind.VOID, [])
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('must follow set* naming convention') }, method)
        0 * messager._

        expect:
        model.hasErrors()
    }

    def 'reports error for method with bad naming convention'() {
        final method = mockExecutableElement('firstName', TypeKind.DECLARED, [])
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('must follow get*/is*/set* naming convention') }, method)
        0 * messager._

        expect:
        model.hasErrors()
    }

    // --- helpers ---

    private TypeElement mockSourceElement(List<ExecutableElement> methods) {
        final source = Mock(TypeElement)
        source.interfaces >> []
        final superclass = Mock(TypeMirror)
        superclass.kind >> TypeKind.NONE
        source.superclass >> superclass
        source.enclosedElements >> methods
        return source
    }

    private ExecutableElement mockGetterMethod(String name, TypeKind returnKind) {
        final method = Mock(ExecutableElement)
        final simpleName = Mock(Name)
        simpleName.toString() >> name
        method.simpleName >> simpleName
        method.parameters >> []
        method.kind >> ElementKind.METHOD
        method.modifiers >> EnumSet.of(Modifier.ABSTRACT, Modifier.PUBLIC)
        final returnType = Mock(TypeMirror)
        returnType.kind >> returnKind
        method.returnType >> returnType
        method.annotationMirrors >> []
        return method
    }

    private ExecutableElement mockSetterMethod(String name) {
        final param = Mock(VariableElement)
        final method = Mock(ExecutableElement)
        final simpleName = Mock(Name)
        simpleName.toString() >> name
        method.simpleName >> simpleName
        method.parameters >> [param]
        method.kind >> ElementKind.METHOD
        method.modifiers >> EnumSet.of(Modifier.ABSTRACT, Modifier.PUBLIC)
        final returnType = Mock(TypeMirror)
        returnType.kind >> TypeKind.VOID
        method.returnType >> returnType
        return method
    }

    private ExecutableElement mockExecutableElement(String name, TypeKind returnKind, List params) {
        final method = Mock(ExecutableElement)
        final simpleName = Mock(Name)
        simpleName.toString() >> name
        method.simpleName >> simpleName
        method.parameters >> params
        method.kind >> ElementKind.METHOD
        method.modifiers >> EnumSet.of(Modifier.ABSTRACT, Modifier.PUBLIC)
        final returnType = Mock(TypeMirror)
        returnType.kind >> returnKind
        method.returnType >> returnType
        return method
    }
}
