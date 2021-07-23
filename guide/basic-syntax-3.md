![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

[이전 포스팅](https://jaime-note.tistory.com/70)에 이어서 Querydsl의 기본 문법을 소개합니다.

### 기본 Join

첫 번 째 파라미터에 `join`할 대상, 두 번 째 파라미터에 별칭으로 사용할 `Q Type`을 지정합니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;

@SpringBootTest
@Transactional
class PlayerTest {
    @Autowired
    EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    @BeforeEach
    void setup() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        queryFactory = new JPAQueryFactory(entityManager);
    }
    
    @Test
    void simpleQuerydslWithJoin() {
        List<Player> players = queryFactory.select(player)
                .from(player)
                .join(player.team, team) // (1)
                .fetch();
        players.forEach(p -> System.out.printf("%s %s%n", p, p.getTeam())); // (2)
    }
}
```

> (1) `join`할 대상은 `player.team`, 대상의 `Q Type`은 `QTeam.team` 입니다.  
> (2) `toString`에서 `Team`을 제외했기 때문에 추가로 출력하기위해 따로 호출하였습니다.  

테스트를 실행해보면,

```text
2021-07-19 10:46:42.900 DEBUG 5815 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player   
    inner join
        player.team as team */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id
Player(id=3, name=Harry Kane, age=27) Team(id=1, name=Tottenham Hotspur F.C.)
Player(id=4, name=Heungmin Son, age=29) Team(id=1, name=Tottenham Hotspur F.C.)
Player(id=5, name=Kevin De Bruyne, age=30) Team(id=2, name=Manchester City F.C.)
Player(id=6, name=Raheem Shaquille Sterling, age=26) Team(id=2, name=Manchester City F.C.)
```

`join` 쿼리가 잘 작성되었고, 쿼리가 1회 수행되었으며, 각 정보를 정확하게 출력하는 것을 확인할 수 있습니다.

`Querydsl`은 내부적으로 `EntityManager`를 이용해 쿼리하기 때문에 `JPQL`로 실행했을 때와 동일한 `SQL`문을 생성하는 것을 확인할 수 있습니다.

그리고 `JPA`에서 `join`의 기본 값은 `inner join` 이기 때문에 다른 `join`을 선택하지 않으면 기본 값으로 동작합니다.

지원하는 `join` 기능은 다음과 같습니다.

* `join()`: `inner join`과 동일, `JPA` 기본 `join`
* `innerJoin()`: inner join 
* `leftJoin()`: left join
* `rightJoin()`: right join
* `fetchJoin()`: 아래서 추가 항목으로 설명

### 세타(theta) join

세타 `join`은 `join`에 참여하는 두 릴레이션의 속성 값을 비교하여 조건을 만족하는 `Tuple`만 반환합니다.

`Querydsl`에서는 `from` 절에 여러 개의 `Entity`를 파라미터로 넘겨주는 방식으로 간단히 구현할 수 있습니다.

예시가 마땅치 않아 `Player Entity`의 `Team` 이름과 `Team Entity`의 이름이 동일한 `Player`를 조회하였습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;

@SpringBootTest
@Transactional
class PlayerTest {
    @Autowired
    EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    @BeforeEach
    void setup() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @Test
    void simpleQuerydslWithThetaJoin() {
        List<Player> players = queryFactory.select(player)
                .from(player, team) // (1)
                .where(player.team.name.eq(team.name)) // (2)
                .fetch();
        players.forEach(p -> System.out.printf("%s %s%n", p, p.getTeam()));
    }
}
```

> (1) `from` 메서드에 비교할 `Entity`를 넘겨줍니다.
> (2) `where` 메서드에 비교할 조건을 넘겨줍니다. 팀 이름이 같은 `row`를 반환하게 하였습니다.

테스트를 실행해보면,

```text
2021-07-19 11:00:11.896 DEBUG 5930 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player,
        Team team 
    where
        player.team.name = team.name */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ cross 
        join
            team team1_ cross 
        join
            team team2_ 
        where
            player0_.team_id=team2_.team_id 
            and team2_.name=team1_.name
Player(id=3, name=Harry Kane, age=27) Team(id=1, name=Tottenham Hotspur F.C.)
Player(id=4, name=Heungmin Son, age=29) Team(id=1, name=Tottenham Hotspur F.C.)
Player(id=5, name=Kevin De Bruyne, age=30) Team(id=2, name=Manchester City F.C.)
Player(id=6, name=Raheem Shaquille Sterling, age=26) Team(id=2, name=Manchester City F.C.)
```

이렇게 `theta join` 문법이 적용된 `SQL`문과 결과를 확인할 수 있습니다.

`theta join`을 사용할 때 주의해야할 점은 외부 `join`(`outer join`)을 할 수 없다는 것 입니다.

`where` 절에 조건을 넣기 때문에 여러 개의 `Entity`에서 해당 조건을 만족하는 경우의 데이터만 획득할 수 있기 때문입니다.

대신 `on` 절을 이용하면 이 부분도 해결할 수 있습니다.

### on 절 사용

`join` 대상을 필터링하거나 연관관계가 없는 `Entity`와의 외부 `join`을 위해 사용합니다.

먼저 `join` 대상을 필터링하기 위해 사용해보겠습니다.

본문이 너무 길어져 나머지 동일한 부분은 생략하겠습니다.

```java
@Test
void simpleQuerydslWithLeftJoinOn() {
    List<Tuple> tuples = queryFactory.select(player, team)
        .from(player)
        .leftJoin(player.team, team) // (1)
        .on(team.name.eq("Tottenham Hotspur F.C.")) // (2)
        .fetch();
    tuples.forEach(System.out::println);
}
```

> (1) `leftJoin`을 이용해 `Player` 기준으로 조회합니다.  
> (2) `on()` 절을 이용해 팀 이름 조건을 추가해줬습니다.  

결과를 확인해보면,

```text
2021-07-19 11:29:20.040 DEBUG 6124 --- [           main] org.hibernate.SQL                        : 
    /* select
        player,
        team 
    from
        Player player   
    left join
        player.team as team with team.name = ?1 */ select
            player0_.player_id as player_i1_1_0_,
            team1_.team_id as team_id1_2_1_,
            player0_.age as age2_1_0_,
            player0_.name as name3_1_0_,
            player0_.team_id as team_id4_1_0_,
            team1_.name as name2_2_1_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
                and (
                    team1_.name=?
                )
[Player(id=3, name=Harry Kane, age=27), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=4, name=Heungmin Son, age=29), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=5, name=Kevin De Bruyne, age=30), null]
[Player(id=6, name=Raheem Shaquille Sterling, age=26), null]
```

`on()`을 사용하지 않았다면 `Player`가 가진 `Team`의 `ID`와, `Team`이 가진 `ID`만 비교했겠지만 `on()` 절에 의해 팀 이름까지 비교하는 것을 확인할 수 있습니다.

`Player` 기준으로 조회했기 때문에 팀 이름이 토트넘이 아닌 `row`는 팀이 없이 조회되었습니다.

`leftJoin` 대신 그냥 `join`을 사용할 경우,

```java
@Test
void simpleQuerydslWithJoinOn() {
    List<Tuple> tuples = queryFactory.select(player, team)
            .from(player)
            .join(player.team, team)
            .on(team.name.eq("Tottenham Hotspur F.C."))
            .fetch();
    tuples.forEach(System.out::println);
}
```

```text
2021-07-19 11:42:23.241 DEBUG 6198 --- [           main] org.hibernate.SQL                        : 
    /* select
        player,
        team 
    from
        Player player   
    inner join
        player.team as team with team.name = ?1 */ select
            player0_.player_id as player_i1_1_0_,
            team1_.team_id as team_id1_2_1_,
            player0_.age as age2_1_0_,
            player0_.name as name3_1_0_,
            player0_.team_id as team_id4_1_0_,
            team1_.name as name2_2_1_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id 
                and (
                    team1_.name=?
                )
[Player(id=3, name=Harry Kane, age=27), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=4, name=Heungmin Son, age=29), Team(id=1, name=Tottenham Hotspur F.C.)]
```

이렇게 팀 이름이 일치하지 않는 `row`는 제외하고 조회하는 것을 확인할 수 있습니다.

여기서 확인할 수 있는 점은 내부 `join`인 경우 굳이 `on` 절을 사용할 필요 없이 `where` 절 내에서 해결할 수 있다는 점 입니다.

```java
@Test
void simpleQuerydslWithJoinWithoutOn() {
    List<Tuple> tuples = queryFactory.select(player, team)
            .from(player)
            .join(player.team, team)
            .where(team.name.eq("Tottenham Hotspur F.C."))
            .fetch();
    tuples.forEach(System.out::println);
}
```

```text
2021-07-19 11:45:00.844 DEBUG 6228 --- [           main] org.hibernate.SQL                        : 
    /* select
        player,
        team 
    from
        Player player   
    inner join
        player.team as team 
    where
        team.name = ?1 */ select
            player0_.player_id as player_i1_1_0_,
            team1_.team_id as team_id1_2_1_,
            player0_.age as age2_1_0_,
            player0_.name as name3_1_0_,
            player0_.team_id as team_id4_1_0_,
            team1_.name as name2_2_1_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=?
[Player(id=3, name=Harry Kane, age=27), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=4, name=Heungmin Son, age=29), Team(id=1, name=Tottenham Hotspur F.C.)]
```

이렇게 했을 경우 쿼리가 훨씬 단순해지고 효율도 더 좋습니다.

따라서 외부 `join`이 필요한 경우가 아니라면 굳이 `on`을 사용할 필요가 없습니다.

마지막으로 외부 `join`을 사용하는 케이스를 확인해보겠습니다.

```java
@Test
void simpleQuerydslWithOuterJoinOn() {
    List<Tuple> tuples = queryFactory.select(player, team)
            .from(player)
            .leftJoin(team)
            .on(player.team.name.eq(team.name))
            .fetch();
    tuples.forEach(System.out::println);
}
```

위에서 이미 테스트했던 것과 매우 유사하지만, `leftJoin`에 `join`을 위한 필드를 넣는 것이 아닌 `Entity` 자체를 전달하고 있습니다.

기존 처럼 `leftJoin(player.team, team)` 이렇게 사용할 경우 애너테이션에 의해 매핑할 필드를 직접 찾아 `join` 하지만 그냥 `Entity`를 바로 전달하였을 경우 `ID`로 매칭하는 것이 아니라 `on` 절의 조건을 비교하여 반환하게 됩니다. 

테스트 결과는 아래와 같습니다.

```text
2021-07-19 11:49:52.617 DEBUG 6277 --- [           main] org.hibernate.SQL                        : 
    /* select
        player,
        team 
    from
        Player player   
    left join
        Team team with player.team.name = team.name */ select
            player0_.player_id as player_i1_1_0_,
            team1_.team_id as team_id1_2_1_,
            player0_.age as age2_1_0_,
            player0_.name as name3_1_0_,
            player0_.team_id as team_id4_1_0_,
            team1_.name as name2_2_1_ 
        from
            player player0_ 
        left outer join
            team team2_ 
                on player0_.team_id=team2_.team_id 
        left outer join
            team team1_ 
                on (
                    team2_.name=team1_.name
                )
[Player(id=3, name=Harry Kane, age=27), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=4, name=Heungmin Son, age=29), Team(id=1, name=Tottenham Hotspur F.C.)]
[Player(id=5, name=Kevin De Bruyne, age=30), Team(id=2, name=Manchester City F.C.)]
[Player(id=6, name=Raheem Shaquille Sterling, age=26), Team(id=2, name=Manchester City F.C.)]
```

`left outer join`이 정상적으로 수행된 것을 확인할 수 있습니다.

`Entity`를 대충 만들다보니 테스트 케이스가 딱 와닿지 않는데요, 좀 더 다양한 필드와 데이터가 존재하는 경우 이 조건에 대해 테스트했을 때 보다 더 정확한 결과를 확인할 수 있습니다.

### fetch join

`fetch join`은 `SQL`에서 제공하는 기능이 아니라 연관된 `Entity`를 `join`하기위해 사용하는 `JPA`의 기능이고 주로 `JPA`의 성능을 최적화하기위해 사용합니다.

바로 테스트 코드를 살펴보면

```java
@Test
void simpleQuerydslWithFetchJoin() {
    Player founded = queryFactory.selectFrom(player)
            .join(player.team, team)
            .fetchJoin() // (1)
            .where(player.name.eq("Heungmin Son"))
            .fetchOne();
    assertNotNull(founded);
    assertNotNull(founded.getTeam());
    System.out.println(founded + " " + founded.getTeam()); // (2)
}
```

> (1) `join` 이후 `fetchJoin()`을 추가로 호출합니다.  
> (2) `getTeam()`을 호출했을 때 추가 쿼리가 없는지 확인합니다.

테스트 결과는 다음과 같습니다.

```text
2021-07-19 12:15:44.987 DEBUG 6526 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player   
    inner join
        fetch player.team as team 
    where
        player.name = ?1 */ select
            player0_.player_id as player_i1_1_0_,
            team1_.team_id as team_id1_2_1_,
            player0_.age as age2_1_0_,
            player0_.name as name3_1_0_,
            player0_.team_id as team_id4_1_0_,
            team1_.name as name2_2_1_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            player0_.name=?
Player(id=4, name=Heungmin Son, age=29) Team(id=1, name=Tottenham Hotspur F.C.)
```

`join`이 제대로 동작하였기 때문에 `getTeam()`을 호출할 때 추가 쿼리가 필요하지 않습니다.

`fetch join`에 대해서는 이미 [이전 포스팅](https://jaime-note.tistory.com/54)에서 다뤘기 때문에 여기서는 추가로 설명하지 않겠습니다.

---

이상으로 `Querydsl`에서 제공하는 `join` 기능에 대해 알아보았습니다.

다음 포스팅에서는 서브 쿼리와 Case문, 상수, 문자 더하기 기능에 대해 다뤄보겠습니다.