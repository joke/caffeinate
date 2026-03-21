package io.github.joke.caffeinate.mutable

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.Property
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

@Subject(MutableFieldStrategy)
class MutableFieldStrategySpec extends Specification {

    final strategy = new MutableFieldStrategy()
    TypeElement source = Mock()

    def 'generates private non-final field for each property'() {
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.fields.size() == 1
        verifyAll(model.fields[0]) {
            name == 'name'
            modifiers.contains(Modifier.PRIVATE)
            !modifiers.contains(Modifier.FINAL)
        }
    }

    def 'propagates annotations to mutable field'() {
        final annotation = AnnotationSpec.builder(Override).build()
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', [annotation]))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.fields[0].annotations.size() == 1
    }

    def 'generates no fields for empty properties'() {
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.fields.empty
    }
}
