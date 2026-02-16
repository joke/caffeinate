package io.github.joke.objects.mutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.immutable.AnalysisPhase;
import io.github.joke.objects.immutable.GenerationPhase;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.GetterStrategy;
import java.util.Set;

@Module
public interface MutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy mutablePropertyDiscovery(
            MutablePropertyDiscoveryStrategy impl);

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy setterValidation(SetterValidationStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy mutableField(MutableFieldStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy getter(GetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy setter(SetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy mutableConstructor(MutableConstructorStrategy impl);
}
