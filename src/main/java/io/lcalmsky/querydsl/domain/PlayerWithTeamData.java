package io.lcalmsky.querydsl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerWithTeamData {
    private String name;
    private int age;
    private String teamName;
}
