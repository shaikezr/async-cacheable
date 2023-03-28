package com.async.cacheable.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.async.cacheable.annotation.AsyncCacheable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AsyncCacheableMethodProcessor implements BeanPostProcessor {

  private final AsyncCacheManager asyncCacheManager;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {

    Arrays.stream(bean.getClass().getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(AsyncCacheable.class))
        .forEach(
            m -> {
              AsyncCacheable asyncCacheable = m.getAnnotation(AsyncCacheable.class);
              String cacheName = asyncCacheable.name();
              long expireAfterWriteSeconds = asyncCacheable.expireAfterWriteSeconds();
              long maximumSize = asyncCacheable.maximumSize();
              String cacheIdentifier =
                  "%s_%s_%s".formatted(cacheName, expireAfterWriteSeconds, maximumSize);
              asyncCacheManager.computeIfAbsent(
                  cacheIdentifier,
                  (key) -> {
                    return Caffeine.newBuilder()
                        .maximumSize(maximumSize)
                        .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
                        .buildAsync();
                  });
            });

    return bean;
  }
}
