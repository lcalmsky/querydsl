![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

`Querydsl`을 이용해 동적 쿼리를 작성하는 방법은 두 가지가 있습니다.

`BooleanBuilder`를 사용하는 방법과 `where` 절에 파라미터를 전달하는 방식이 있는데요, 하나씩 살펴보도록 하겠습니다.

### BooleanBuilder

`BooleanBuilder`는 `Predicate`를 구현하는 구현체이고 `Predicate`는 `where`절의 파라미터 타입입니다.

따라서 `BooleanBuilder`를 이용해 조건절을 추가한 뒤 `where`절에 전달하면되고, 이 부분을 동적으로 구현할 수 있습니다.

현재 `Entity`는 필드 수가 워낙 적어 경우의 수가 몇 가지 나오지 않으니 한 번 모두 테스트해보도록 하겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    void simpleQuerydslWithBooleanBuilder() {
        // given
        List<QueryParam> queryParams = new ArrayList<>(); // (1)
        queryParams.add(QueryParam.of("Heungmin Son", 29));
        queryParams.add(QueryParam.of("Heungmin Son", null));
        queryParams.add(QueryParam.of(null, 29));
        queryParams.add(QueryParam.of(null, null));

        // when
        for (QueryParam queryParam : queryParams) { // (2)
            List<Player> players = queryFactory
                    .selectFrom(player)
                    .where(whereClause(queryParam)) 
                    .fetch();
            players.forEach(System.out::println);
        }
    }

    private Predicate whereClause(QueryParam queryParam) { // (3)
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        Optional.ofNullable(queryParam.getName())
                .ifPresent(name -> booleanBuilder.and(player.name.eq(name)));
        Optional.ofNullable(queryParam.getAge())
                .ifPresent(age -> booleanBuilder.and(player.age.lt(age)));
        return booleanBuilder;
    }
}
```

> (1) 쿼리에 사용할 파라미터를 이름과 나이 둘 다 가지는 경우, 이름만 가지는 경우, 나이만 가지는 경우, 둘 다 가지지 않는 경우, 네 가지 케이스로 생성합니다.  
> (2) 네 가지 경우 모두 테스트하기 위해 반복문 안에서 처리하였습니다.  
> (3) `where` 절을 `BooleanBuilder`를 이용해 구성합니다. 파라미터의 존재 여부에 따라 `BooleanBuilder`에 `and` 조건으로 추가하였습니다.  

테스트를 실행해보면 네 가지 결과가 나오는데 실제로 파라미터의 유무에 따라 `where`절이 생성되었는지 확인해보겠습니다.

```text
2021-07-23 09:14:06.813 DEBUG 8035 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name = ?1 
        and player.age < ?2 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where // (1)
            player0_.name=? 
            and player0_.age<?
2021-07-23 09:14:06.821 DEBUG 8035 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name = ?1 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where // (2)
            player0_.name=?
Player(id=4, name=Heungmin Son, age=29)
2021-07-23 09:14:06.846 DEBUG 8035 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.age < ?1 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where // (3)
            player0_.age<?
Player(id=3, name=Harry Kane, age=27)
Player(id=6, name=Raheem Shaquille Sterling, age=26)
2021-07-23 09:14:06.852 DEBUG 8035 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_
        // (4)
Player(id=3, name=Harry Kane, age=27)
Player(id=4, name=Heungmin Son, age=29)
Player(id=5, name=Kevin De Bruyne, age=30)
Player(id=6, name=Raheem Shaquille Sterling, age=26)
```

> (1) `name`, `age` 조건이 모두 생성되었습니다.  
> (2) `name` 조건만 생성되었습니다.  
> (3) `age` 조건만 생성되었습니다.  
> (4) 두 조건이 모두 없으므로 `where` 절이 생성되지 않았습니다.  

where 메서드는 `Predicate`가 모두 `null`일 경우 생성되지 않아 파라미터 존재 여부에 따라 자유롭게 조건을 구성할 수 있습니다.

### where 메서드 파라미터 사용

바로 위에 언급했듯이 `where` 절에는 `Predicate`를 0개에서 N개까지 전달 가능합니다.

`BooleanBuilder`를 이용했을 경우 N개가 하나로 합쳐진 `Predicate` 한 개를 전달하는 방식인데요, 여러 개를 전달할 경우 아래처럼 구현할 수 있습니다.

```java
@Test
void simpleQuerydslWithDynamicQueryUsingWhereClause() {
    // given
    List<QueryParam> queryParams = new ArrayList<>();
    queryParams.add(QueryParam.of("Heungmin Son", 29));
    queryParams.add(QueryParam.of("Heungmin Son", null));
    queryParams.add(QueryParam.of(null, 29));
    queryParams.add(QueryParam.of(null, null));

    // when
    for (QueryParam queryParam : queryParams) {
        List<Player> players = queryFactory
                .selectFrom(player)
                .where(Optional.ofNullable(queryParam.getName()) // (1)
                                .map(player.name::eq)
                                .orElse(null),
                        Optional.ofNullable(queryParam.getAge()) // (2)
                                .map(player.age::lt)
                                .orElse(null))
                .fetch();
        players.forEach(System.out::println);
    }
}
```

> (1) 쿼리 파라미터중 `name`이 있을 경우 `player.name`과 비교하고 없으면 `null`을 전달합니다.  
> (1) 쿼리 파라미터중 `age`이 있을 경우 `player.age`과 비교하고 없으면 `null`을 전달합니다.  

`BooleanBuilder`를 사용했을 때와 동일한 결과를 확인할 수 있습니다.

```text
2021-07-23 09:28:45.716 DEBUG 8086 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name = ?1 
        and player.age < ?2 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.name=? 
            and player0_.age<?
2021-07-23 09:28:45.728 DEBUG 8086 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name = ?1 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.name=?
Player(id=4, name=Heungmin Son, age=29)
2021-07-23 09:28:45.758 DEBUG 8086 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.age < ?1 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.age<?
Player(id=3, name=Harry Kane, age=27)
Player(id=6, name=Raheem Shaquille Sterling, age=26)
2021-07-23 09:28:45.764 DEBUG 8086 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_
Player(id=3, name=Harry Kane, age=27)
Player(id=4, name=Heungmin Son, age=29)
Player(id=5, name=Kevin De Bruyne, age=30)
Player(id=6, name=Raheem Shaquille Sterling, age=26)
```

`Optional`이 너무 지저분하다고 느껴지시면 메서드로 추출하여 더 간단하게 표현할 수 있습니다.

```java
@Test
void simpleQuerydslWithDynamicQueryUsingWhereClause() {
    // given
    List<QueryParam> queryParams = new ArrayList<>();
    queryParams.add(QueryParam.of("Heungmin Son", 29));
    queryParams.add(QueryParam.of("Heungmin Son", null));
    queryParams.add(QueryParam.of(null, 29));
    queryParams.add(QueryParam.of(null, null));

    // when
    for (QueryParam queryParam : queryParams) {
        List<Player> players = queryFactory
                .selectFrom(player)
                .where(condition(queryParam.getName(), player.name::eq),
                        condition(queryParam.getAge(), player.age::lt))
                .fetch();
        players.forEach(System.out::println);
    }
}

private <T> Predicate condition(T value, Function<T, Predicate> function) {
    return Optional.ofNullable(value)
            .map(function)
            .orElse(null);
}
```

단, 이렇게 전달하게되면 모든 조건이 `and`로 묶이게 됩니다. 

`where`절에 `Predicate`를 여러 개 넘길 경우 기본 동작이 `and`이기 때문인데요, where 절 구현 내용을 쭉 따라서 올라가보면,

**QueryBase.java**
```java
public Q where(Predicate... o) {
    return queryMixin.where(o);
}
```

**QueryMixin.java**
```java
public final T where(Predicate... o) {
    for (Predicate e : o) {
        metadata.addWhere(convert(e, Role.WHERE));
    }
    return self;
}
```

**DefaultQueryMetadata.java**
```java
@Override
public void addWhere(Predicate e) {
    if (e == null) {
        return;
    }
    e = (Predicate) ExpressionUtils.extract(e);
    if (e != null) {
        validate(e);
        where = and(where, e);
    }
}
```

이렇게 마지막에 `and`를 호출하는 것을 확인할 수 있습니다.

따라서 `or`를 사용하려면 어쩔 수 없이 `BooleanBuilder`를 사용해야 합니다.

보통 동적 쿼리를 사용할 때는 `and` 조건을 사용하는 경우가 많기 때문에 `or`를 기본 조건으로 사용하는 등의 예외상황만 따로 처리한다면 위 방식으로 사용하는데 큰 문제는 없을 거 같습니다.

---

다음 포스팅에서는 벌크 연산에 대해 다뤄보겠습니다. 😁