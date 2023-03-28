package com.async.cacheable.exception;

public class CacheException extends Exception {
  public CacheException(String errorMessage, Throwable err) {
    super(errorMessage, err);
  }
}
