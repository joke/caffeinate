package io.github.joke.caffeinate.mutable

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import io.github.joke.caffeinate.strategy.ClassModel
import io.github.joke.caffeinate.strategy.Property
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.TypeElement

@Subject(SetterStrategy)
class SetterStrategySpec extends Specification {

    final strategy = new SetterStrategy()
    TypeElement source = Mock()

    def 'generates public void setter with correct name and parameter'() {
        final model = new ClassModel()
        model.properties.add(new Property('firstName', TypeName.get(String), 'getFirstName', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.size() == 1
        final setter = model.methods[0].toString()
        setter.contains('setFirstName')
        setter.contains('public')
        setter.contains('void')
    }

    def 'setter parameter has correct type and name'() {
        final model = new ClassModel()
        model.properties.add(new Property('age', TypeName.INT, 'getAge', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        verifyAll(model.methods[0].parameters[0]) {
            name == 'age'
            type == TypeName.INT
        }
    }

    def 'propagates annotations to setter parameter'() {
        final annotation = AnnotationSpec.builder(Override).build()
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', [annotation]))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods[0].toString().contains('@java.lang.Override')
    }

    def 'generates no setters for empty properties'() {
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.empty
    }
}
