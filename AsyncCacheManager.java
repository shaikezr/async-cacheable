package com.async.cacheable.manager;

import com.github.benmanes.caffeine.cache.AsyncCache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Aspect
@Component
public class AsyncCacheManager {
  private final Map<String, AsyncCache> cacheMap = new ConcurrentHashMap<>();

  public AsyncCache get(String name) {
    return this.cacheMap.get(name);
  }

  public AsyncCache computeIfAbsent(String name, Function mappingFunction) {
    return this.cacheMap.computeIfAbsent(name, mappingFunction);
  }
}
