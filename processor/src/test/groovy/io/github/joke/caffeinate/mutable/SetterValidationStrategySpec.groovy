package io.github.joke.caffeinate.mutable

import com.palantir.javapoet.TypeName
import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.Property
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.processing.Messager
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor
import javax.tools.Diagnostic

@Subject(SetterValidationStrategy)
class SetterValidationStrategySpec extends Specification {

    Messager messager = Mock()
    final strategy = new SetterValidationStrategy(messager)
    TypeElement source = Mock()

    def 'passes when setter matches a property by name and type'() {
        final stringType = mockDeclaredType('String', 'java.lang.String')
        final propertyTypeName = TypeName.get(stringType)

        final model = new ClassModel()
        model.properties.add(new Property('firstName', propertyTypeName, 'getFirstName', []))
        model.declaredSetters.add(mockSetter('setFirstName', stringType))

        when:
        strategy.generate(source, model)

        then:
        0 * messager.printMessage(Diagnostic.Kind.ERROR, *_)
        0 * _

        expect:
        !model.hasErrors()
    }

    def 'reports error when setter does not match any property by name'() {
        final stringType = mockDeclaredType('String', 'java.lang.String')
        final propertyTypeName = TypeName.get(stringType)

        final model = new ClassModel()
        model.properties.add(new Property('firstName', propertyTypeName, 'getFirstName', []))
        final setter = mockSetter('setLastName', stringType)
        model.declaredSetters.add(setter)

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, { it.contains('does not match any getter-derived property') }, setter)
        0 * _

        expect:
        model.hasErrors()
    }

    def 'reports error when setter has wrong parameter type'() {
        final stringType = mockDeclaredType('String', 'java.lang.String')
        final intType = mockDeclaredType('Integer', 'java.lang.Integer')
        final propertyTypeName = TypeName.get(stringType)

        final model = new ClassModel()
        model.properties.add(new Property('firstName', propertyTypeName, 'getFirstName', []))
        final setter = mockSetter('setFirstName', intType)
        model.declaredSetters.add(setter)

        when:
        strategy.generate(source, model)

        then:
        1 * messager.printMessage(Diagnostic.Kind.ERROR, _, setter)
        0 * _

        expect:
        model.hasErrors()
    }

    def 'passes validation with no declared setters'() {
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * messager.printMessage(*_)
        0 * _

        expect:
        !model.hasErrors()
    }

    private DeclaredType mockDeclaredType(String simpleName, String qualifiedName) {
        final typeElement = Stub(TypeElement)
        final simpleNameObj = Stub(Name)
        simpleNameObj.toString() >> simpleName
        typeElement.simpleName >> simpleNameObj
        final qualifiedNameObj = Stub(Name)
        qualifiedNameObj.toString() >> qualifiedName
        typeElement.qualifiedName >> qualifiedNameObj
        typeElement.kind >> ElementKind.CLASS

        final packageElement = Stub(PackageElement)
        final packageName = Stub(Name)
        packageName.toString() >> qualifiedName.substring(0, qualifiedName.lastIndexOf('.'))
        packageElement.qualifiedName >> packageName
        packageElement.simpleName >> packageName
        packageElement.kind >> ElementKind.PACKAGE
        packageElement.enclosingElement >> null
        packageElement.accept(*_) >> { ElementVisitor visitor, Object p -> visitor.visitPackage(packageElement, p) }

        typeElement.enclosingElement >> packageElement
        typeElement.accept(*_) >> { ElementVisitor visitor, Object p -> visitor.visitType(typeElement, p) }

        final noType = Stub(javax.lang.model.type.NoType)
        noType.kind >> TypeKind.NONE

        final declaredType = Stub(DeclaredType)
        declaredType.kind >> TypeKind.DECLARED
        declaredType.asElement() >> typeElement
        declaredType.typeArguments >> []
        declaredType.enclosingType >> noType
        declaredType.accept(*_) >> { TypeVisitor visitor, Object p -> visitor.visitDeclared(declaredType, p) }

        return declaredType
    }

    private ExecutableElement mockSetter(String name, DeclaredType paramType) {
        final setter = Stub(ExecutableElement)
        final simpleName = Stub(Name)
        simpleName.toString() >> name
        setter.simpleName >> simpleName

        final param = Stub(VariableElement)
        param.asType() >> paramType
        setter.parameters >> [param]

        return setter
    }
}
