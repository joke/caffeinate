package io.github.joke.caffeinate.mutable

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.Property
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@Subject(MutableConstructorStrategy)
class MutableConstructorStrategySpec extends Specification {

    final strategy = new MutableConstructorStrategy()

    def 'generates only no-args constructor when properties are empty'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.size() == 1
        model.methods[0].constructor
        model.methods[0].parameters.empty
    }

    def 'generates no-args and all-args constructors when properties exist'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.size() == 2
        model.methods[0].constructor
        model.methods[0].parameters.empty
        model.methods[1].constructor
        model.methods[1].parameters.size() == 1
    }

    def 'adds super() for non-interface source in both constructors'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.CLASS
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods[0].toString().contains('super()')
        model.methods[1].toString().contains('super()')
    }

    def 'does not add super() for interface source'() {
        final source = Stub(TypeElement)
        source.kind >> ElementKind.INTERFACE
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        !model.methods[0].toString().contains('super()')
        !model.methods[1].toString().contains('super()')
    }

    def 'propagates annotations to all-args constructor parameters'() {
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
        model.methods[1].parameters[0].annotations.size() == 1
    }
}
