package io.github.joke.caffeinate.mutable

import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.GenerationStrategy
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.processing.Filer
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject

@Subject(MutableGenerator)
class MutableGeneratorSpec extends Specification {

    GenerationStrategy analysisStrategy = Mock()
    GenerationStrategy validationStrategy = Mock()
    GenerationStrategy generationStrategy = Mock()
    Filer filer = Mock()

    final generator = new MutableGenerator(
            [analysisStrategy] as Set,
            [validationStrategy] as Set,
            [generationStrategy] as Set,
            filer
    )

    def 'runs analysis then validation then generation in order'() {
        final source = mockTypeElement('Person')
        final order = []

        when:
        generator.generate(source)

        then:
        1 * analysisStrategy.generate(source, _) >> { TypeElement s, ClassModel m ->
            order << 'analysis'
            m.className = 'PersonImpl'
            m.modifiers.add(Modifier.PUBLIC)
        }
        1 * validationStrategy.generate(source, _) >> { order << 'validation' }
        1 * generationStrategy.generate(source, _) >> { order << 'generation' }
        1 * filer.createSourceFile(*_) >> mockJavaFileObject()
        0 * _

        expect:
        order == ['analysis', 'validation', 'generation']
    }

    def 'short-circuits when model has errors after validation'() {
        final source = mockTypeElement('Person')

        when:
        generator.generate(source)

        then:
        1 * analysisStrategy.generate(source, _) >> { TypeElement s, ClassModel m ->
            m.className = 'PersonImpl'
        }
        1 * validationStrategy.generate(source, _) >> { TypeElement s, ClassModel m ->
            m.hasErrors = true
        }
        0 * generationStrategy.generate(*_)
        0 * filer.createSourceFile(*_)
        0 * _
    }

    def 'short-circuits when model has errors after analysis'() {
        final source = mockTypeElement('Person')

        when:
        generator.generate(source)

        then:
        1 * analysisStrategy.generate(source, _) >> { TypeElement s, ClassModel m ->
            m.hasErrors = true
        }
        1 * validationStrategy.generate(*_)
        0 * generationStrategy.generate(*_)
        0 * filer.createSourceFile(*_)
        0 * _
    }

    def 'writes generated file to filer'() {
        final source = mockTypeElement('Person')

        when:
        generator.generate(source)

        then:
        1 * analysisStrategy.generate(source, _) >> { TypeElement s, ClassModel m ->
            m.className = 'PersonImpl'
            m.modifiers.add(Modifier.PUBLIC)
        }
        1 * validationStrategy.generate(source, _)
        1 * generationStrategy.generate(source, _)
        1 * filer.createSourceFile(*_) >> mockJavaFileObject()
        0 * _
    }

    private TypeElement mockTypeElement(String simpleName) {
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
        element.kind >> ElementKind.INTERFACE

        final qualifiedName = Stub(Name)
        qualifiedName.toString() >> "test.${simpleName}"
        element.qualifiedName >> qualifiedName
        element.enclosingElement >> packageElement
        element.accept(*_) >> { ElementVisitor visitor, Object p -> visitor.visitType(element, p) }

        return element
    }

    private JavaFileObject mockJavaFileObject() {
        Stub(JavaFileObject) {
            openWriter() >> new StringWriter()
        }
    }
}
