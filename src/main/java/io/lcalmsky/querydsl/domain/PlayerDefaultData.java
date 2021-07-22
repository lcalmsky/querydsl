package io.lcalmsky.querydsl.domain;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PlayerDefaultData {
    private String name;
    private int age;

    @QueryProjection
    public PlayerDefaultData(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
