package io.github.joke.objects.mutable;

import dagger.Subcomponent;

@Subcomponent(modules = MutableModule.class)
public interface MutableSubcomponent {

    MutableGenerator generator();

    @Subcomponent.Factory
    interface Factory {
        MutableSubcomponent create();
    }
}
