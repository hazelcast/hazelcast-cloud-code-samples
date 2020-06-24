package com.hazelcast.cloud.mapstore;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonMapStoreTest {

    private static final String MAP_NAME = "person";

    @ClassRule
    public static final MySQLContainer<?> mysql = new MySQLContainer<>()
        .withUsername("mapstore")
        .withPassword("maploader")
        .withInitScript("person_table.sql");

    private static HazelcastInstance hazelcast;

    private static HikariDataSource dataSource;

    @BeforeClass
    public static void init() {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", mysql.getDriverClassName());
        properties.setProperty("jdbcUrl", mysql.getJdbcUrl());
        properties.setProperty("username", mysql.getUsername());
        properties.setProperty("password", mysql.getPassword());

        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setClassName(PersonMapStore.class.getCanonicalName());
        mapStoreConfig.setProperties(properties);
        mapStoreConfig.setWriteDelaySeconds(0);
        XmlConfigBuilder configBuilder = new XmlConfigBuilder();
        Config config = configBuilder.build();
        MapConfig mapConfig = config.getMapConfig(MAP_NAME);
        mapConfig.setMapStoreConfig(mapStoreConfig);

        hazelcast = Hazelcast.newHazelcastInstance(config);
        dataSource = new HikariDataSource(new HikariConfig(properties));

    }

    @AfterClass
    public static void shutdown() {
        if (hazelcast != null) {
            hazelcast.shutdown();
        }
        if (dataSource != null) {
            dataSource.close();
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
        return new JdbcPersonRepository(dataSource);
    }

}
