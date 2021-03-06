package io.lcalmsky.querydsl.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.Optional;

@Table(name = "Player")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Player {
    @Id
    @GeneratedValue
    @Column(name = "player_id")
    private Long id;
    private String name;
    private int age;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @ToString.Exclude
    private Team team;
    private Boolean inSeason;
    private Integer weeklySalary;

    public Player(String name) {
        this(name, 0, null);
    }

    public Player(String name, int age) {
        this(name, age, null);
    }

    public Player(String name, int age, Team team) {
        this.name = name;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
        this.inSeason = false;
        this.weeklySalary = 0;
    }

    private void changeTeam(Team team) {
        Optional.ofNullable(this.team).ifPresent(t -> t.removeIfExist(this));
        this.team = team;
        team.getPlayers().add(this);
    }

    public void begins() {
        this.inSeason = true;
    }

    public void over() {
        this.inSeason = false;
    }

    public void contactSalary(int weeklySalary) {
        this.weeklySalary = weeklySalary;
    }

    public void raiseSalary(float rate) {
        this.weeklySalary = (int) (weeklySalary + weeklySalary * rate);
    }
}
