package com.hazelcast.cloud.mapstore;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mysql {@link MapStore} implementation.
 */
@Slf4j
public class PersonMapStore implements MapStore<Integer, Person>, MapLoaderLifecycleSupport, Serializable {

    private HikariDataSource dataSource;

    private PersonRepository personRepository;

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        this.dataSource = new HikariDataSource(new HikariConfig(properties));
        this.personRepository = new JdbcPersonRepository(this.dataSource);
        log.info("PersonMapStore::initialized");
    }

    @Override
    public void destroy() {
        HikariDataSource dataSource = this.dataSource;
        if (dataSource != null) {
            dataSource.close();
        }
        log.info("PersonMapStore::destroyed");
    }

    @Override
    public void store(Integer key, Person value) {
        log.info("PersonMapStore::store key {} value {}", key, value);
        getRepository().save(Person.builder()
            .id(key)
            .name(value.getName())
            .lastname(value.getLastname())
            .build());
    }

    @Override
    public void storeAll(Map<Integer, Person> map) {
        log.info("PersonMapStore::store all {}", map);
        for (Map.Entry<Integer, Person> entry : map.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void delete(Integer key) {
        log.info("PersonMapStore::delete key {}", key);
        getRepository().delete(key);
    }

    @Override
    public void deleteAll(Collection<Integer> keys) {
        log.info("PersonMapStore::delete all {}", keys);
        getRepository().delete(keys);
    }

    @Override
    public Person load(Integer key) {
        log.info("PersonMapStore::load by key {}", key);
        return getRepository().find(key).orElse(null);
    }

    @Override
    public Map<Integer, Person> loadAll(Collection<Integer> keys) {
        log.info("PersonMapStore::loadAll by keys {}", keys);
        return getRepository().findAll(keys).stream()
            .collect(Collectors.toMap(Person::getId, Function.identity()));
    }

    @Override
    public Iterable<Integer> loadAllKeys() {
        log.info("PersonMapStore::loadAllKeys");
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
