package io.github.joke.objects

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import spock.lang.Specification

import static com.google.testing.compile.Compiler.javac

class MutableProcessorSpec extends Specification {

    def 'generates mutable implementation for @Mutable interface with single getter'() {
        given:
        def source = JavaFileObjects.forSourceString('test.Person', '''\
            package test;
            import io.github.joke.objects.Mutable;
            @Mutable
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
        generated.contains('public class PersonImpl implements Person')
        generated.contains('private String firstName')
        !generated.contains('private final String firstName')
        generated.contains('public PersonImpl()')
        generated.contains('public PersonImpl(String firstName)')
        generated.contains('this.firstName = firstName')
        generated.contains('public String getFirstName()')
        generated.contains('return this.firstName')
        generated.contains('public void setFirstName(String firstName)')
    }
}
