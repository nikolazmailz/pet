package com.pet.rest.redis;

public interface RedisAdapter {
    void putToQueue(String queue, String value);
    String getStringFromDb(String key);
    void putStringToDb(String key, String value, int ttlSeconds);
}
