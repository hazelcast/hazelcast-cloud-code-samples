package com.hazelcast.cloud.mapstore4.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;

@Slf4j
public class JdbcPersonMapStore implements MapStore<Integer, Person>, MapLoaderLifecycleSupport {

    private HikariDataSource dataSource;

    private PersonRepository personRepository;

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        this.dataSource = new HikariDataSource(new HikariConfig(properties));
        this.personRepository = new JdbcPersonRepository(this.dataSource);
        log.info("JdbcPersonMapStore::initialized");
    }

    @Override
    public void destroy() {
        HikariDataSource dataSource = this.dataSource;
        if (dataSource != null) {
            dataSource.close();
        }
        log.info("JdbcPersonMapStore::destroyed");
    }

    @Override
    public void store(Integer key, Person value) {
        log.info("JdbcPersonMapStore::store key {} value {}", key, value);
        getRepository().save(Person.builder()
            .id(key)
            .name(value.getName())
            .lastname(value.getLastname())
            .build());
    }

    @Override
    public void storeAll(Map<Integer, Person> map) {
        log.info("JdbcPersonMapStore::store all {}", map);
        for (Map.Entry<Integer, Person> entry : map.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void delete(Integer key) {
        log.info("JdbcPersonMapStore::delete key {}", key);
        getRepository().delete(key);
    }

    @Override
    public void deleteAll(Collection<Integer> keys) {
        log.info("JdbcPersonMapStore::delete all {}", keys);
        getRepository().delete(keys);
    }

    @Override
    public Person load(Integer key) {
        log.info("JdbcPersonMapStore::load by key {}", key);
        return getRepository().find(key).orElse(null);
    }

    @Override
    public Map<Integer, Person> loadAll(Collection<Integer> keys) {
        log.info("JdbcPersonMapStore::loadAll by keys {}", keys);
        return getRepository().findAll(keys).stream()
            .collect(Collectors.toMap(Person::getId, Function.identity()));
    }

    @Override
    public Iterable<Integer> loadAllKeys() {
        log.info("JdbcPersonMapStore::loadAllKeys");
        return getRepository().findAllIds();
    }

    private PersonRepository getRepository() {
        PersonRepository personRepository = this.personRepository;
        if (personRepository == null) {
            throw new IllegalStateException("PersonRepository must not be null!");
        }
        return this.personRepository;
    }

}
