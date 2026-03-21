package io.github.joke.caffeinate.strategy

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

@Subject(FieldStrategy)
class FieldStrategySpec extends Specification {

    final strategy = new FieldStrategy()
    TypeElement source = Mock()

    def 'generates private final field for each property'() {
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
            modifiers.contains(Modifier.FINAL)
        }
    }

    def 'propagates annotations to field'() {
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

    def 'generates multiple fields for multiple properties'() {
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', []))
        model.properties.add(new Property('age', TypeName.INT, 'getAge', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.fields.size() == 2
        model.fields[0].name == 'name'
        model.fields[1].name == 'age'
    }
}
