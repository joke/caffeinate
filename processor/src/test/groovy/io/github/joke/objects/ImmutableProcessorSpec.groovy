package io.github.joke.objects

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import spock.lang.Specification

import static com.google.testing.compile.Compiler.javac

class ImmutableProcessorSpec extends Specification {

    def 'generates implementation for @Immutable interface'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.objects.Immutable;
            @Immutable
            public interface Person {}
        ''')

        when:
        def compilation = javac()
            .withProcessors(new ObjectsProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS
        compilation.generatedSourceFile('test.PersonImpl').isPresent()

        and:
        def generated = compilation.generatedSourceFile('test.PersonImpl')
            .get().getCharContent(true).toString()
        generated.contains('package test;')
        generated.contains('public class PersonImpl implements Person')
    }

    def 'fails when @Immutable applied to a class'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.objects.Immutable;
            @Immutable
            public class Person {}
        ''')

        when:
        def compilation = javac()
            .withProcessors(new ObjectsProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('@Immutable can only be applied to interfaces')
        }
    }

    def 'generates field, getter, and constructor for single getter method'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.objects.Immutable;
            @Immutable
            public interface Person {
                String getFirstName();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new ObjectsProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS

        and:
        def generated = compilation.generatedSourceFile('test.PersonImpl')
            .get().getCharContent(true).toString()
        generated.contains('private final String firstName')
        generated.contains('public PersonImpl(String firstName)')
        generated.contains('this.firstName = firstName')
        generated.contains('public String getFirstName()')
        generated.contains('return this.firstName')
    }

    def 'fails when @Immutable applied to an enum'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Color', '''\
            package test;
            import io.github.joke.objects.Immutable;
            @Immutable
            public enum Color { RED, GREEN, BLUE }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new ObjectsProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('@Immutable can only be applied to interfaces')
        }
    }
}
