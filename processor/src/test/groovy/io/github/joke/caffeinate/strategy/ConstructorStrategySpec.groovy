package io.github.joke.caffeinate.strategy

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@Subject(ConstructorStrategy)
class ConstructorStrategySpec extends Specification {

    final strategy = new ConstructorStrategy()

    def 'generates constructor with parameters for properties'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.size() == 1
        final constructor = model.methods[0].toString()
        constructor.contains('public')
        constructor.contains('name')
    }

    def 'does not generate constructor when properties are empty'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.empty
    }

    def 'adds super() for non-interface source'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.CLASS
        final model = new ClassModel()
        model.properties.add(new Property('id', TypeName.get(String), 'getId', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods[0].toString().contains('super()')
    }

    def 'does not add super() for interface source'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()
        model.properties.add(new Property('id', TypeName.get(String), 'getId', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        !model.methods[0].toString().contains('super()')
    }

    def 'propagates annotations to constructor parameters'() {
        final annotation = AnnotationSpec.builder(Override).build()
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', [annotation]))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods[0].toString().contains('@java.lang.Override')
    }
}
