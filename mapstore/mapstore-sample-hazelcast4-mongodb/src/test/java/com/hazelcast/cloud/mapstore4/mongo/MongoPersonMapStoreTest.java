package com.hazelcast.cloud.mapstore4.mongo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoPersonMapStoreTest {

    public static final int MONGO_PORT = 27017;

    @ClassRule
    public static final GenericContainer<?> MONGO_DB_CONTAINER = new GenericContainer<>("mongo:latest")
        .withExposedPorts(MONGO_PORT);

    private static final String MAP_NAME = "person";

    private static HazelcastInstance hazelcast;

    private static MongoClient mongoClient;

    @BeforeClass
    public static void init() {
        String uri = String.format("mongodb://%s:%d/test?retryWrites=true&w=majority",
            MONGO_DB_CONTAINER.getHost(), MONGO_DB_CONTAINER.getMappedPort(MONGO_PORT));
        Properties properties = new Properties();
        properties.setProperty("uri", uri);
        properties.setProperty("database", "test");
        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setClassName(MongoPersonMapStore.class.getCanonicalName());
        mapStoreConfig.setProperties(properties);
        mapStoreConfig.setWriteDelaySeconds(0);
        Config config = new XmlConfigBuilder().build();
        MapConfig mapConfig = config.getMapConfig(MAP_NAME);
        mapConfig.setMapStoreConfig(mapStoreConfig);
        hazelcast = Hazelcast.newHazelcastInstance(config);
        mongoClient = new MongoClient(new MongoClientURI(uri));
    }

    @AfterClass
    public static void shutdown() {
        if (hazelcast != null) {
            hazelcast.shutdown();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Before
    @After
    public void clearAll() {
        getMap().clear();
        getRepository().deleteAll();
    }

    @Test
    public void store() {
        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        getMap().put(1, john);
        getMap().put(2, iosef);

        assertThat(getRepository().find(1)).hasValue(john);
        assertThat(getRepository().find(2)).hasValue(iosef);

    }

    @Test
    public void storeAll() {
        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        Map<Integer, Person> persons = new LinkedHashMap<>();
        persons.put(1, john);
        persons.put(2, iosef);
        getMap().putAll(persons);

        assertThat(getRepository().find(1)).hasValue(john);
        assertThat(getRepository().find(2)).hasValue(iosef);

    }

    @Test
    public void delete() {
        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        getMap().put(1, john);
        getMap().put(2, iosef);

        assertThat(getRepository().find(1)).hasValue(john);
        assertThat(getRepository().find(2)).hasValue(iosef);

        getMap().remove(1);
        getMap().remove(2);

        assertThat(getRepository().find(1)).isEmpty();
        assertThat(getRepository().find(2)).isEmpty();

    }

    @Test
    public void deleteAll() {
        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        getMap().put(1, john);
        getMap().put(2, iosef);

        assertThat(getRepository().find(1)).hasValue(john);
        assertThat(getRepository().find(2)).hasValue(iosef);

        getMap().clear();

        assertThat(getRepository().find(1)).isEmpty();
        assertThat(getRepository().find(2)).isEmpty();

    }

    @Test
    public void load() {

        assertThat(getMap().get(1)).isNull();

        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        getRepository().save(john);
        getRepository().save(iosef);

        assertThat(getMap().get(1)).isEqualTo(john);
        assertThat(getMap().get(2)).isEqualTo(iosef);

    }

    @Test
    public void loadAll() {
        Person john = Person.builder()
            .id(1)
            .name("John")
            .lastname("Wick")
            .build();

        Person iosef = Person.builder()
            .id(2)
            .name("Iosef")
            .lastname("Tarasov")
            .build();

        getRepository().save(john);
        getRepository().save(iosef);

        getMap().loadAll(true);

        getRepository().deleteAll();

        assertThat(getMap().get(1)).isEqualTo(john);
        assertThat(getMap().get(2)).isEqualTo(iosef);

    }

    private static IMap<Integer, Person> getMap() {
        return hazelcast.getMap(MAP_NAME);
    }

    private static PersonRepository getRepository() {
        return new MongoPersonRepository(MAP_NAME, mongoClient.getDatabase("test"));
    }

}
