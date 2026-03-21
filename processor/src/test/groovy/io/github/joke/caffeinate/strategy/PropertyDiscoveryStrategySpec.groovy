package io.github.joke.caffeinate.strategy

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

@Subject(PropertyDiscoveryStrategy)
class PropertyDiscoveryStrategySpec extends Specification {

    Messager messager = Mock()
    Types types = Mock()
    final resolver = new TypeHierarchyResolver(types)
    final strategy = new PropertyDiscoveryStrategy(messager, resolver)

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

    def 'reports error for method with parameters'() {
        final param = Mock(VariableElement)
        final method = mockExecutableElement('getName', TypeKind.DECLARED, [param])
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

    def 'reports error for void return type'() {
        final method = mockExecutableElement('doSomething', TypeKind.VOID, [])
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('must not return void') }, method)
        0 * messager._

        expect:
        model.hasErrors()
    }

    def 'reports error for bad naming convention'() {
        final method = mockExecutableElement('firstName', TypeKind.DECLARED, [])
        final source = mockSourceElement([method])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('must follow get*/is* naming convention') }, method)
        0 * messager._

        expect:
        model.hasErrors()
    }

    def 'processes multiple methods correctly'() {
        final getter = mockGetterMethod('getName', TypeKind.DECLARED)
        final badMethod = mockExecutableElement('firstName', TypeKind.DECLARED, [])
        final source = mockSourceElement([getter, badMethod])
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, _, badMethod)
        0 * messager._

        expect:
        model.properties.size() == 1
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
