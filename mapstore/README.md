# Hazelcast MapStore/MapLoader Samples

## Overview

Hazelcast allows you to load and store the distributed map entries from/to a persistent data store such as a relational database. 

To do this, you can use Hazelcast’s `MapStore` and `MapLoader` interfaces. 

### Terminology
| Term  | Definition |
| ------------- | ------------- |
| MapLoader | [MapLoader](https://github.com/hazelcast/hazelcast/blob/master/hazelcast/src/main/java/com/hazelcast/map/MapLoader.java) is an SPI. When you provide a MapLoader implementation and request an entry (using IMap.get()) that does not exist in memory, MapLoader's load method loads that entry from the data store. This loaded entry is placed into the map and will stay there until it is removed or evicted.  |
| MapStore  | [MapStore](https://github.com/hazelcast/hazelcast/blob/master/hazelcast/src/main/java/com/hazelcast/map/MapStore.java)  is also an SPI. When a MapStore implementation is provided, an entry is also put into a user defined data store. MapStore extends MapLoader. Later in this document, by MapStore we mean both MapStore and MapLoader since they compose a full-featured MapStore CRUD SPI.  |
| MapStore Configuration  | MapStore Configuration defines a MapStore implementation (class name or instance) and properties which control the way it works.  |


## Samples
 
- [Hazelcast 3.X.X MapStore JDBC Sample](mapstore-sample-hazelcast3-jdbc/README.md)
- [Hazelcast 4.X.X MapStore JDBC Sample](mapstore-sample-hazelcast4-jdbc/README.md)
- [Hazelcast 3.X.X MapStore Mongo Sample](mapstore-sample-hazelcast3-mongodb/README.md)
- [Hazelcast 4.X.X MapStore Mongo Sample](mapstore-sample-hazelcast4-mongodb/README.md)

## How to run?

MapStore Samples can be easily built with the maven wrapper. You also need JDK 1.8.

- Clone the Git repository.
- Open the project directory
- `./mvnw clean verify` to build and run all samples 


## Reference

https://docs.hazelcast.org/docs/latest/manual/html-single/index.html#loading-and-storing-persistent-data



## F.A.Q.

- Why do we need to use `maven-shade-plugin`?
> In order to make sure, that all MapStore dependencies are available in the final JAR. 
