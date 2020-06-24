package com.hazelcast.cloud.mapstore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface PersonRepository {

    void save(Person person);

    default void delete(Integer id) {
        delete(Collections.singletonList(id));
    }

    void deleteAll();

    void delete(Collection<Integer> keys);

    List<Person> findAll(Collection<Integer> ids);

    Collection<Integer> findAllIds();

    default Optional<Person> find(Integer key) {
        return findAll(Collections.singletonList(key)).stream().findFirst();
    }

}
