# @AsyncCacheable

## Overview:

Caching annotation used to cache Mono/Flux values. Uses an async loading cache to offer resilience to cache stampedes. Converts Mono/Flux to CompleteableFuture prior to upserting into the cache, and converts from CompleteableFuture back to Mono/Flux after retrieval from the cache.

NOTE (March 5, 2024): Spring recently added support for Reactive types through `@Cacheable`, so that is the preferred approach now. Use Spring's out-of-the-box `@Cacheable` annotation instead of this. See: https://github.com/spring-projects/spring-framework/issues/17920

## Usage:

Simply annotate a function returning Mono/Flux with `@AsyncCacheable`.

## Parameters:

Each cache instance is a hash of it's parameters. Annotations with the same parameters will use the same cache. Annotations with different parameters will use different caches. The parameters are:

```
name: String, a name for the cache. Default name is "default1000Item5MinuteCache"
maximumSize: Long, maximum number of items in the cache. Default maximumSize is 1000
expireAfterWriteSeconds: Long, number of seconds after writing to the cache to expire items. Default is 300
```

These parameters can be overridden by specifying the value you want in your annotation invocation. Ex:
```
    @AsyncCacheable
    public Mono<String> myCachedMono() {
        System.out.println("Uncached response");
        return Mono.just("hi");
    }

    public Mono<String> myUnCachedMono() {
        System.out.println("Uncached response");
        return Mono.just("hey");
    }

    @AsyncCacheable(name="customName", maximumSize=100L, expireAfterWriteSeconds=50L)
    public Mono<String> myCustomCachedMono() {
        System.out.println("Uncached response");
        return Mono.just("hey");
    }
```

## Acknowledgements:
1. https://github.com/ben-manes/caffeine/discussions/500
2. https://gist.github.com/bijukunjummen/a12ab5d3e823c5f052ce608b5fc7b6a4
