package com.async.cacheable.manager;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.async.cacheable.exception.CacheException;
import com.async.cacheable.annotation.AsyncCacheable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Aspect
@Component
public class AsyncCacheAspect {

  private final AsyncCacheManager asyncCacheManager;

  @Pointcut("@annotation(AsyncCacheable)")
  public void pointcut() {}

  @Around("pointcut()")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable, CacheException {
    System.out.println("inside of pointcut");
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    String methodHash = "%s".formatted(method.hashCode());

    ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
    Type rawType = parameterizedType.getRawType();

    if (!rawType.equals(Mono.class) && !rawType.equals(Flux.class)) {
      throw new IllegalArgumentException(
          "The return type is not Mono/Flux. Use Mono/Flux for return type. method: "
              + method.getName());
    }

    AsyncCacheable asyncCacheable = method.getAnnotation(AsyncCacheable.class);
    String cacheIdentifier =
        "%s_%s_%s"
            .formatted(
                asyncCacheable.name(),
                asyncCacheable.expireAfterWriteSeconds(),
                asyncCacheable.maximumSize());
    Object[] args = joinPoint.getArgs();

    AsyncCache asyncCache = asyncCacheManager.get(cacheIdentifier);
    if (Objects.isNull(asyncCache)) {
      return joinPoint.proceed();
    }

    // Return type : Mono
    if (rawType.equals(Mono.class)) {
      Mono retVal =
          Mono.defer(
              () -> {
                try {
                  return (Mono) joinPoint.proceed();
                } catch (Throwable th) {
                  //you can throw some custom business exception instead of throwable
                  return Mono.error(new CacheException("Error processing async cache with Mono", th));
                }
              });
      CompletableFuture completableFuture =
          asyncCache.get(generateKey(methodHash, args), (key, exec) -> (retVal).toFuture());
      return Mono.fromFuture(completableFuture);
    }

    // Return type : Flux
    Mono retVal =
        Mono.from(
            Flux.defer(
                () -> {
                  try {
                    return ((Flux) joinPoint.proceed()).collectList();
                  } catch (Throwable th) {
                    return Mono.error(new CacheException("Error processing async cache with Flux", th));
                  }
                }));

    CompletableFuture<List> completableFuture =
        asyncCache.get(generateKey(methodHash, args), (key, exec) -> (retVal).toFuture());
    return Flux.from(Mono.fromFuture(completableFuture)).flatMap(x -> Flux.fromIterable(x));
  }

  private static String generateKey(String methodHash, Object... objects) {
    return methodHash
        + Arrays.stream(objects)
            .map(obj -> obj == null ? "" : obj.toString())
            .collect(Collectors.joining("#"));
  }
}
