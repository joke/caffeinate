package io.github.joke.objects.immutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import java.util.Set;

@Module
public interface ImmutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);
}
