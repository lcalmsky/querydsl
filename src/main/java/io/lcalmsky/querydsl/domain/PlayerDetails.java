package io.lcalmsky.querydsl.domain;

import lombok.Data;

@Data
public class PlayerDetails {
    private String name;
    private Integer age;
    private boolean inSeason;
    private Integer weeklySalary;
    private String teamName;
}
