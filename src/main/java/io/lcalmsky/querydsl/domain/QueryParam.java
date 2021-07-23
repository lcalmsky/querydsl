package io.lcalmsky.querydsl.domain;

import lombok.Data;

@Data(staticConstructor = "of")
public class QueryParam {
    private final String name;
    private final Integer age;
}
