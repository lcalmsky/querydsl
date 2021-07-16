package io.lcalmsky.querydsl.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Team {
    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;
    @OneToMany(mappedBy = "team")
    @ToString.Exclude
    private List<Player> players = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

    public void removeIfExist(Player player) {
        players.removeIf(p -> p.equals(player));
    }
}