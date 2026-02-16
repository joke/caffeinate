package io.github.joke.caffeinate

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import spock.lang.Specification

import static com.google.testing.compile.Compiler.javac

class ImmutableProcessorSpec extends Specification {

    def 'generates implementation for @Immutable interface'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {}
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
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
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public class Person {}
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
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
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {
                String getFirstName();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
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

    def 'generates fields and constructor for multiple getter methods'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {
                String getFirstName();
                int getAge();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS

        and:
        def generated = compilation.generatedSourceFile('test.PersonImpl')
            .get().getCharContent(true).toString()
        generated.contains('private final String firstName')
        generated.contains('private final int age')
        generated.contains('public String getFirstName()')
        generated.contains('public int getAge()')
        generated.contains('PersonImpl(String firstName, int age)')
    }

    def 'generates field from boolean is* getter'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Status', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Status {
                boolean isActive();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS

        and:
        def generated = compilation.generatedSourceFile('test.StatusImpl')
            .get().getCharContent(true).toString()
        generated.contains('private final boolean active')
        generated.contains('public boolean isActive()')
        generated.contains('return this.active')
    }

    def 'fails when method has parameters'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {
                String getName(int x);
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('must have no parameters')
        }
    }

    def 'fails when method returns void'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {
                void doSomething();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('must not return void')
        }
    }

    def 'fails when method name does not follow get/is convention'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public interface Person {
                String firstName();
            }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('must follow get*/is* naming convention')
        }
    }

    def 'fails when @Immutable applied to an enum'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Color', '''\
            package test;
            import io.github.joke.caffeinate.Immutable;
            @Immutable
            public enum Color { RED, GREEN, BLUE }
        ''')

        when:
        def compilation = javac()
            .withProcessors(new CaffeinateProcessor())
            .compile(source)

        then:
        compilation.status() == Compilation.Status.FAILURE
        compilation.errors().any {
            it.getMessage(null).contains('@Immutable can only be applied to interfaces')
        }
    }
}
