# Hazelcast 4.X.X MapStore MongoDB Sample

This is an example of how to use `MapStore` and `MongoDB`.

Required properties:
 - `uri` - Mongo connection string. See more here: https://docs.mongodb.com/manual/reference/connection-string/
 - `database` - The database 
 
## Classes

- [Person](src/main/java/com/hazelcast/cloud/mapstore4/mongo/Person.java) Entity class
- [MongoPersonRepository](src/main/java/com/hazelcast/cloud/mapstore4/mongo/MongoPersonRepository.java) MongoDB implementation for Person Entity.
- [PersonMapStore](src/main/java/com/hazelcast/cloud/mapstore4/mongo/MongoPersonMapStore.java) MapStore implementation for Person Entity.
- [PersonMapStoreTest](src/test/java/com/hazelcast/cloud/mapstore4/mongo/MongoPersonMapStoreTest.java) Person MapStore tests.

