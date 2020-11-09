package com.hazelcast.cloud.mapstore4.mongo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Person implements Serializable {

    private Integer id;

    private String name;

    private String lastname;

}
