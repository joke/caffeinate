package io.github.joke.caffeinate.strategy

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.TypeName
import spock.lang.Specification
import spock.lang.Subject

import javax.lang.model.element.TypeElement

@Subject(GetterStrategy)
class GetterStrategySpec extends Specification {

    final strategy = new GetterStrategy()
    TypeElement source = Mock()

    def 'generates getter method with @Override and correct return type'() {
        final model = new ClassModel()
        model.properties.add(new Property('age', TypeName.INT, 'getAge', []))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.size() == 1
        final getter = model.methods[0].toString()
        getter.contains('getAge')
        getter.contains('@java.lang.Override')
        getter.contains('public')
    }

    def 'propagates annotations to getter method'() {
        final annotation = AnnotationSpec.builder(Override).build()
        final model = new ClassModel()
        model.properties.add(new Property('name', TypeName.get(String), 'getName', [annotation]))

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods[0].toString().count('@java.lang.Override') >= 2
    }

    def 'generates no methods for empty properties'() {
        final model = new ClassModel()

        when:
        strategy.generate(source, model)

        then:
        0 * _

        expect:
        model.methods.empty
    }
}
