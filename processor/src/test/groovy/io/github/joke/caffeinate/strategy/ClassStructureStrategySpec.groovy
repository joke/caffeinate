package io.github.joke.caffeinate.strategy

import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

@Subject(ClassStructureStrategy)
class ClassStructureStrategySpec extends Specification {

    final strategy = new ClassStructureStrategy()

    def 'sets class name with Impl suffix and adds PUBLIC modifier'() {
        final source = mockTypeElement('Person', ElementKind.INTERFACE)
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.className == 'PersonImpl'
        model.modifiers.contains(Modifier.PUBLIC)
    }

    def 'adds superinterface for interface source'() {
        final source = mockTypeElement('Person', ElementKind.INTERFACE)
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.superinterfaces.size() == 1
        model.superclass == null
    }

    def 'sets superclass for abstract class source'() {
        final source = mockTypeElement('AbstractEntity', ElementKind.CLASS)
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.superclass != null
        model.superinterfaces.empty
    }

    private TypeElement mockTypeElement(String simpleName, ElementKind kind) {
        final packageElement = Stub(PackageElement)
        final packageName = Stub(Name)
        packageName.toString() >> 'test'
        packageElement.qualifiedName >> packageName
        packageElement.simpleName >> packageName
        packageElement.kind >> ElementKind.PACKAGE
        packageElement.enclosingElement >> null
        packageElement.accept(*_) >> { ElementVisitor visitor, Object p -> visitor.visitPackage(packageElement, p) }

        final element = Stub(TypeElement)
        final name = Stub(Name)
        name.toString() >> simpleName
        element.simpleName >> name
        element.kind >> kind

        final qualifiedName = Stub(Name)
        qualifiedName.toString() >> "test.${simpleName}"
        element.qualifiedName >> qualifiedName
        element.enclosingElement >> packageElement
        element.accept(*_) >> { ElementVisitor visitor, Object p -> visitor.visitType(element, p) }

        return element
    }
}
