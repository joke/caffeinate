package io.github.joke.objects;

import com.google.auto.service.AutoService;
import io.github.joke.objects.component.DaggerProcessorComponent;
import io.github.joke.objects.component.ProcessorModule;
import io.github.joke.objects.immutable.ImmutableSubcomponent;
import io.github.joke.objects.mutable.MutableSubcomponent;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class ObjectsProcessor extends AbstractProcessor {

    private ImmutableSubcomponent immutableSubcomponent;
    private MutableSubcomponent mutableSubcomponent;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        var component = DaggerProcessorComponent.builder()
                .processorModule(new ProcessorModule(processingEnv))
                .build();
        immutableSubcomponent = component.immutable().create();
        mutableSubcomponent = component.mutable().create();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                Immutable.class.getCanonicalName(),
                Mutable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            String annotationName = annotation.getQualifiedName().toString();

            for (Element element :
                    roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "@" + annotation.getSimpleName()
                                            + " can only be applied"
                                            + " to interfaces",
                                    element);
                    continue;
                }
                try {
                    if (annotationName.equals(
                            Immutable.class.getCanonicalName())) {
                        immutableSubcomponent
                                .generator()
                                .generate((TypeElement) element);
                    } else if (annotationName.equals(
                            Mutable.class.getCanonicalName())) {
                        mutableSubcomponent
                                .generator()
                                .generate((TypeElement) element);
                    }
                } catch (IOException e) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Failed to generate implementation: "
                                            + e.getMessage(),
                                    element);
                }
            }
        }
        return false;
    }
}
