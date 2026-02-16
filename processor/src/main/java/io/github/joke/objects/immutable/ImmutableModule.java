package io.github.joke.objects.immutable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.joke.objects.strategy.ClassStructureStrategy;
import io.github.joke.objects.strategy.ConstructorStrategy;
import io.github.joke.objects.strategy.FieldStrategy;
import io.github.joke.objects.strategy.GenerationStrategy;
import io.github.joke.objects.strategy.GetterStrategy;
import io.github.joke.objects.strategy.PropertyDiscoveryStrategy;
import java.util.Set;

@Module
public interface ImmutableModule {

    @Multibinds
    @AnalysisPhase
    Set<GenerationStrategy> analysisStrategies();

    @Binds
    @IntoSet
    @AnalysisPhase
    GenerationStrategy propertyDiscovery(PropertyDiscoveryStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy classStructure(ClassStructureStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy field(FieldStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy getter(GetterStrategy impl);

    @Binds
    @IntoSet
    @GenerationPhase
    GenerationStrategy constructor(ConstructorStrategy impl);
}
