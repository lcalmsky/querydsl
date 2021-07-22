package io.lcalmsky.querydsl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData {
    private String playerName;
    private int playerAge;
}
