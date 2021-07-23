![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

[이전 포스팅](https://jaime-note.tistory.com/69)에 이어서 Querydsl의 기본 문법을 소개합니다.

### 정렬

`JPAQueryFactory`에서 `orderBy` 메서드를 호출해 정렬 기능을 사용합니다.

`orderBy`의 파라미터로 정렬할 항목들을 전달하는데 아래 테스트 코드처럼 작성하면 됩니다.

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
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PlayerTest {
    @Autowired
    EntityManager entityManager;

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
    }

    @Test
    void simpleQuerydslWithSortTest() {
        // given
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Player> players = queryFactory.selectFrom(player)
                .orderBy(player.age.desc(), player.name.asc().nullsLast())
                .fetch();
        // then
        assertEquals("Kevin De Bruyne", players.get(0).getName());
        assertEquals("Raheem Shaquille Sterling", players.get(3).getName());
    }
}
```

나이는 내림차순으로, 이름은 오름차순으로 정렬하였고 이름이 없을 경우 마지막에 나타나게 하였습니다.

테스트를 실행해보면,

```text
2021-07-18 03:30:43.998 DEBUG 2231 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    order by
        player.age desc,
        player.name asc nulls last */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        order by
            player0_.age desc,
            player0_.name asc nulls last
```

원하는 쿼리가 실행된 것을 확인할 수 있습니다.

`nullsLast()`(`null`인 항목을 마지막으로)와 `nullsFirst()`(`null`인 항목을 처음으로) 두 가지 기능만 따로 알아놓으시면 나머지는 쿼리 작성할 때 이미 많이 사용해보셨기 때문에 쉽게 사용 가능합니다.

### 페이징

페이징 처리를 위해선 `offset()`과 `limit()`를 사용합니다. 스프링 데이터 JPA의 `Pageable` 인터페이스를 전달하는 방식보다는 다소 투박(?) 하다고 볼 순 있지만 동적쿼리를 작성하면서 페이징이 필요한 경우 유용하게 사용할 수 있습니다.

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
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PlayerTest {
    @Autowired
    EntityManager entityManager;

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
    }

    @Test
    void simpleQuerydslWithPaging() {
        // given
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Player> players = queryFactory.selectFrom(player)
                .orderBy(player.name.asc())
                .offset(1)
                .limit(2)
                .fetch();
        // then
        assertEquals(2, players.size());
    }
}
```

테스트를 실행해보면 `offset`과 `limit`가 제대로 사용된 것을 확인할 수 있습니다.

```text
2021-07-18 03:40:47.862 DEBUG 2404 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    order by
        player.name asc */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        order by
            player0_.name asc limit ? offset ?
```

이전 포스팅에서 페이징 결과 매핑에 사용할 수 있는 `fetchResults()`를 이용하기위해 마지막을 수정해서 다시 테스트해보겠습니다.

```java
@Test
void simpleQuerydslWithPaging2() {
    // given
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    QueryResults<Player> players = queryFactory.selectFrom(player)
            .orderBy(player.name.asc())
            .offset(1)
            .limit(2)
            .fetchResults();
    // then
    assertEquals(4, players.getTotal());
    assertEquals(2, players.getResults().size());
}
```

```text
2021-07-18 03:46:53.357 DEBUG 2446 --- [           main] org.hibernate.SQL                        : 
    /* select
        count(player) 
    from
        Player player */ select
            count(player0_.player_id) as col_0_0_ 
        from
            player player0_
2021-07-18 03:46:53.371 DEBUG 2446 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    order by
        player.name asc */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        order by
            player0_.name asc limit ? offset ?
```

`totalCount`를 위한 쿼리 1회, `limit`와 `offset`을 이용한 쿼리 1회, 총 2회 쿼리가 발생하는 것을 확인할 수 있습니다.

페이징을 위한 쿼리가 매우 복잡할 경우 `totalCount`를 위한 쿼리는 분리하여 작성하신 뒤 호출하는 것이 더 나은 상황들이 있습니다.

스프링 데이터 JPA에서 `count` 쿼리를 따로 작성하는 것 처럼 간단한 방법을 따로 지원하지 않기 때문에 `fetchResults()` 대신 `fetch()`와 `fetchCount()`로 분리해서 각각 호출한 뒤 결과를 가공해서 반환하는 방식으로 처리해야 합니다.

### 함수

SQL에서의 함수와 동일하게 사용할 수 있습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;

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
    void simpleQuerydslWithFunction() {
        Tuple players = queryFactory.select(player.count(), player.age.sum(), player.age.avg(), player.age.max(), player.age.min())
                .from(player)
                .fetchOne();
        System.out.println(players);
    }
}
```

`count()`, `sum()`, `avg()`, `max()`, `min()` 등 사용하고자 하는 함수를 `select()` 메서드 안에 파라미터로 전달하면 아주 쉽게 원하는 쿼리를 작성할 수 있습니다.

이 때 결과는 `Tuple`이라는 인터페이스로 반환됩니다.

`Tuple` 자체는 실무에서는 거의 사용하지 않지만 사용법은 나중에 다시 다룰 예정입니다.

테스트를 실행해서 쿼리 및 결과만 확인해보면,

```text
2021-07-18 03:58:27.552 DEBUG 2565 --- [           main] org.hibernate.SQL                        : 
    /* select
        count(player),
        sum(player.age),
        avg(player.age),
        max(player.age),
        min(player.age) 
    from
        Player player */ select
            count(player0_.player_id) as col_0_0_,
            sum(player0_.age) as col_1_0_,
            avg(cast(player0_.age as double)) as col_2_0_,
            max(player0_.age) as col_3_0_,
            min(player0_.age) as col_4_0_ 
        from
            player player0_
[4, 112, 28.0, 30, 26]
```

이렇게 함수들이 정상 동작한 것을 확인할 수 있습니다.

### 집합(group by, having)

함수와 같이 사용하려면 `group by`나 `having` 절이 필수겠죠. 이 부분도 아주 간단히 해결할 수 있습니다.

팀별로 선수들의 평균 나이를 구하는 테스트를 작성해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.Tuple;
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
    void simpleQueryDslWithAggregation() {
        List<Tuple> ages = queryFactory.select(team.name, player.age.avg())
                .from(player)
                .join(player.team, team)
                .groupBy(team.name)
                .having(player.age.avg().goe(28))
                .fetch();
        System.out.println(ages);
    }
}
```

`player` `Entity`에선 `age`를 `select`해서 평균을 구하고, `team` `Entity`에서는 이름만 `select` 하도록 하였고, `join()`을 이용해 `player`의 `team`과 `team` `Entity`를 매핑하였습니다.

그리고 마지막으로 `team` `Entity`의 `name`으로 `group by`하여 팀 이름별로 평균 나이를 획득하도록 하였고 평균 나이가 28세 이상인 팀만 추려내기위해 `having`절을 사용하였습니다.

테스트를 실행하면,

```text
2021-07-18 04:17:36.283 DEBUG 2684 --- [           main] org.hibernate.SQL                        : 
    /* select
        team.name,
        avg(player.age) 
    from
        Player player   
    inner join
        player.team as team 
    group by
        team.name 
    having
        avg(player.age) >= ?1 */ select
            team1_.name as col_0_0_,
            avg(cast(player0_.age as double)) as col_1_0_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        group by
            team1_.name 
        having
            avg(cast(player0_.age as double))>=?
[[Manchester City F.C., 28.0], [Tottenham Hotspur F.C., 28.0]]
```

쿼리가 잘 적용되고 결과 또한 정확히 출력되는 것을 확인할 수 있습니다.

---

다음 포스팅에선 `join` 문법에 대해 다루겠습니다.