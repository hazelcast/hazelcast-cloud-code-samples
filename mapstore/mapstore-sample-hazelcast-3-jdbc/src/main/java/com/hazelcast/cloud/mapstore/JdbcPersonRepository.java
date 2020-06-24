package com.hazelcast.cloud.mapstore;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;

@Slf4j
public class JdbcPersonRepository implements PersonRepository, Serializable {

    private final DataSource dataSource;

    public JdbcPersonRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Person person) {
        try (Connection connection = this.dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement("INSERT INTO person (id, name, lastname) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE name=VALUES(name),lastname=VALUES(lastname);")) {
                preparedStatement.setInt(1, person.getId());
                preparedStatement.setString(2, person.getName());
                preparedStatement.setString(3, person.getLastname());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                log.error("Could not obtain a PreparedStatement", ex);
            }
        } catch (SQLException ex) {
            log.error("Could not obtain a Connection", ex);
        }
    }

    @Override
    public void deleteAll() {
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement preparedStatement = connection.createStatement()) {
                preparedStatement.executeUpdate("DELETE FROM person");
            } catch (SQLException ex) {
                log.error("Could not obtain a PreparedStatement", ex);
            }
        } catch (SQLException ex) {
            log.error("Could not obtain a Connection", ex);
        }
    }

    @Override
    public void delete(Collection<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            try (Connection connection = this.dataSource.getConnection()) {
                StringBuilder query = new StringBuilder("DELETE FROM person WHERE id in (");
                for (int i = 0; i < ids.size() - 1; i++) {
                    query.append("?, ");
                }
                query.append("?)");
                try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
                    int index = 1;
                    for (Integer id : ids) {
                        preparedStatement.setInt(index++, id);
                    }
                    preparedStatement.executeUpdate();
                } catch (SQLException ex) {
                    log.error("Could not obtain a PreparedStatement", ex);
                }
            } catch (SQLException ex) {
                log.error("Could not obtain a Connection", ex);
            }
        }

    }

    @Override
    public List<Person> findAll(Collection<Integer> ids) {
        List<Person> result = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            try (Connection connection = this.dataSource.getConnection()) {
                StringBuilder query = new StringBuilder("SELECT * FROM person WHERE id in (");
                for (int i = 0; i < ids.size() - 1; i++) {
                    query.append("?, ");
                }
                query.append("?)");
                try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
                    int index = 1;
                    for (Integer id : ids) {
                        preparedStatement.setInt(index++, id);
                    }
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            result.add(Person.builder()
                                    .id(resultSet.getInt("id"))
                                    .name(resultSet.getString("name"))
                                    .lastname(resultSet.getString("lastname"))
                                    .build());
                        }
                    } catch (SQLException ex) {
                        log.error("Could not obtain a ResultSet", ex);
                    }
                } catch (SQLException ex) {
                    log.error("Could not obtain a PreparedStatement", ex);
                }
            } catch (SQLException ex) {
                log.error("Could not obtain a Connection", ex);
            }
        }
        return result;
    }

    @Override
    public Collection<Integer> findAllIds() {
        List<Integer> result = new ArrayList<>();
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT id FROM person")) {
                    while (resultSet.next()) {
                        result.add(resultSet.getInt("id"));
                    }
                } catch (SQLException ex) {
                    log.error("Could not obtain a ResultSet", ex);
                }
            } catch (SQLException ex) {
                log.error("Could not obtain a PreparedStatement", ex);
            }
        } catch (SQLException ex) {
            log.error("Could not obtain a Connection", ex);
        }
        return result;
    }

}
