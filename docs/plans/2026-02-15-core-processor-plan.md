# Core Annotation Processor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement an annotation processor that generates `<Name>Impl` implementation classes for interfaces annotated with `@Immutable`.

**Architecture:** Dagger subcomponent-per-annotation-type with pluggable generation strategies operating on a mutable `ClassModel`. See `docs/plans/2026-02-15-core-processor-design.md` for full design.

**Tech Stack:** Dagger 2 (DI), Palantir JavaPoet (code generation), Google Auto Service (processor registration), Spock + Google Compile Testing (tests)

---

### Task 1: Build Configuration

**Files:**
- Modify: `settings.gradle:13` (uncomment annotations include)
- Modify: `processor/build.gradle` (fix Dagger dependency, add annotations)
- Delete: `processor/src/main/java/io/github/joke/objects/Test.java`

**Step 1: Uncomment `annotations` module in `settings.gradle`**

Change line 13 from:
```
// include 'annotations'
```
to:
```
include 'annotations'
```

**Step 2: Fix `processor/build.gradle` dependencies**

The current `build.gradle` has `dagger` only as `annotationProcessor`. Dagger annotations (`@Component`, `@Module`, `@Inject`, etc.) need to be on the compile classpath too. Replace the full dependencies block with:

```groovy
dependencies {
    annotationProcessor platform(project(':dependencies'))
    annotationProcessor 'com.google.auto.service:auto-service'
    annotationProcessor 'com.google.dagger:dagger-compiler'

    compileOnly 'com.google.auto.service:auto-service-annotations'

    implementation platform(project(':dependencies'))
    implementation 'com.google.dagger:dagger'
    implementation 'com.palantir.javapoet:javapoet'
    implementation project(':annotations')

    testImplementation 'org.spockframework:spock-core'
    testImplementation 'com.google.testing.compile:compile-testing'
}
```

Key changes:
- Moved `dagger` from `annotationProcessor` to `implementation` (annotations need compile classpath)
- Removed duplicate `dagger` from `annotationProcessor` (only compiler needed there)
- Added `implementation project(':annotations')` for `@Immutable` access

**Step 3: Delete the placeholder file**

Delete `processor/src/main/java/io/github/joke/objects/Test.java`.

**Step 4: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add settings.gradle processor/build.gradle
git rm processor/src/main/java/io/github/joke/objects/Test.java
git commit -m "build: enable annotations module and fix processor dependencies"
```

---

### Task 2: @Immutable Interface Generates Implementation Class

**Files:**
- Test: `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/GenerationStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`
- Create: `processor/src/main/java/io/github/joke/objects/strategy/ClassStructureStrategy.java`
- Create: `processor/src/main/java/io/github/joke/objects/component/ProcessorModule.java`
- Create: `processor/src/main/java/io/github/joke/objects/component/ProcessorComponent.java`
- Create: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`
- Create: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableSubcomponent.java`
- Create: `processor/src/main/java/io/github/joke/objects/immutable/ImmutableGenerator.java`
- Create: `processor/src/main/java/io/github/joke/objects/ObjectsProcessor.java`

**Step 1: Write the failing test**

Create `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`:

```groovy
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
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :processor:test --tests 'io.github.joke.objects.ImmutableProcessorSpec'`
Expected: FAIL — `ObjectsProcessor` class does not exist

**Step 3: Implement the processor**

Create all source files below.

`processor/src/main/java/io/github/joke/objects/strategy/GenerationStrategy.java`:

```java
package io.github.joke.objects.strategy;

import javax.lang.model.element.TypeElement;

public interface GenerationStrategy {
    void generate(TypeElement source, ClassModel model);
}
```

`processor/src/main/java/io/github/joke/objects/strategy/ClassModel.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

public class ClassModel {

    private String className = "";
    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<TypeName> superinterfaces = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public List<TypeName> getSuperinterfaces() {
        return superinterfaces;
    }
}
```

`processor/src/main/java/io/github/joke/objects/strategy/ClassStructureStrategy.java`:

```java
package io.github.joke.objects.strategy;

import com.palantir.javapoet.ClassName;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ClassStructureStrategy implements GenerationStrategy {

    @Inject
    ClassStructureStrategy() {}

    @Override
    public void generate(TypeElement source, ClassModel model) {
        model.setClassName(source.getSimpleName() + "Impl");
        model.getModifiers().add(Modifier.PUBLIC);
        model.getSuperinterfaces().add(ClassName.get(source));
    }
}
```

`processor/src/main/java/io/github/joke/objects/component/ProcessorModule.java`:

```java
package io.github.joke.objects.component;

import dagger.Module;
import dagger.Provides;
import io.github.joke.objects.immutable.ImmutableSubcomponent;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

@Module(subcomponents = ImmutableSubcomponent.class)
public class ProcessorModule {

    private final ProcessingEnvironment processingEnvironment;

    public ProcessorModule(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    @Provides
    Filer filer() {
        return processingEnvironment.getFiler();
    }

    @Provides
    Messager messager() {
        return processingEnvironment.getMessager();
    }
}
```

`processor/src/main/java/io/github/joke/objects/component/ProcessorComponent.java`:

```java
package io.github.joke.objects.component;

import dagger.Component;
import io.github.joke.objects.immutable.ImmutableSubcomponent;

@Component(modules = ProcessorModule.class)
public interface ProcessorComponent {
    ImmutableSubcomponent.Factory immutable();
}
```

`processor/src/main/java/io/github/joke/objects/immutable/ImmutableModule.java`:

```java
package io.github.joke.objects.immutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;

@Module
public interface ImmutableModule {

    @Binds
    @IntoSet
    GenerationStrategy classStructure(ClassStructureStrategy impl);
}
```

`processor/src/main/java/io/github/joke/objects/immutable/ImmutableSubcomponent.java`:

```java
package io.github.joke.objects.immutable;

import dagger.Subcomponent;

@Subcomponent(modules = ImmutableModule.class)
public interface ImmutableSubcomponent {

    ImmutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        ImmutableSubcomponent create();
    }
}
```

`processor/src/main/java/io/github/joke/objects/immutable/ImmutableGenerator.java`:

```java
package io.github.joke.objects.immutable;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.joke.objects.strategy.ClassModel;
import io.github.joke.objects.strategy.GenerationStrategy;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ImmutableGenerator {

    private final Set<GenerationStrategy> strategies;
    private final Filer filer;

    @Inject
    ImmutableGenerator(Set<GenerationStrategy> strategies, Filer filer) {
        this.strategies = strategies;
        this.filer = filer;
    }

    public void generate(TypeElement source) throws IOException {
        ClassModel model = new ClassModel();
        for (GenerationStrategy strategy : strategies) {
            strategy.generate(source, model);
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(model.getClassName());
        for (Modifier modifier : model.getModifiers()) {
            builder.addModifier(modifier);
        }
        for (TypeName superinterface : model.getSuperinterfaces()) {
            builder.addSuperinterface(superinterface);
        }
        TypeSpec typeSpec = builder.build();

        ClassName sourceClass = ClassName.get(source);
        JavaFile javaFile = JavaFile.builder(sourceClass.packageName(), typeSpec).build();
        javaFile.writeTo(filer);
    }
}
```

`processor/src/main/java/io/github/joke/objects/ObjectsProcessor.java`:

```java
package io.github.joke.objects;

import com.google.auto.service.AutoService;
import io.github.joke.objects.component.DaggerProcessorComponent;
import io.github.joke.objects.component.ProcessorModule;
import io.github.joke.objects.immutable.ImmutableSubcomponent;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class ObjectsProcessor extends AbstractProcessor {

    private ImmutableSubcomponent immutableSubcomponent;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        var component = DaggerProcessorComponent.builder()
                .processorModule(new ProcessorModule(processingEnv))
                .build();
        immutableSubcomponent = component.immutable().create();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Immutable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "@Immutable can only be applied to interfaces",
                            element
                    );
                    continue;
                }
                try {
                    immutableSubcomponent.generator().generate((TypeElement) element);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to generate implementation: " + e.getMessage(),
                            element
                    );
                }
            }
        }
        return true;
    }
}
```

Note: `DaggerProcessorComponent` is generated by Dagger's annotation processor during compilation. It won't exist until the first build.

**Step 4: Run test to verify it passes**

Run: `./gradlew :processor:test --tests 'io.github.joke.objects.ImmutableProcessorSpec'`
Expected: PASS — `PersonImpl` generated with correct content

If the Palantir JavaPoet builder API differs from what's written (e.g., immutable builders that return new instances), adjust the `ImmutableGenerator` accordingly.

**Step 5: Commit**

```bash
git add processor/src/
git commit -m "feat: implement core annotation processor for @Immutable interfaces"
```

---

### Task 3: Validation — @Immutable Only on Interfaces

**Files:**
- Modify: `processor/src/test/groovy/io/github/joke/objects/ImmutableProcessorSpec.groovy`

**Step 1: Write the failing tests**

Add these tests to `ImmutableProcessorSpec.groovy`:

```groovy
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
```

**Step 2: Run tests to verify they pass**

The validation logic is already implemented in `ObjectsProcessor.process()` from Task 2 (the `ElementKind.INTERFACE` check). These tests should pass immediately.

Run: `./gradlew :processor:test`
Expected: ALL PASS (3 tests)

If they fail, debug and fix.

**Step 3: Commit**

```bash
git add processor/src/test/
git commit -m "test: add validation tests for @Immutable on non-interface types"
```
