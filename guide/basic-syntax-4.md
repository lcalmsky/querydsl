![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

[이전 포스팅](https://jaime-note.tistory.com/71)에 이어서 `Querydsl`의 기본 문법을 소개합니다.

### 서브 쿼리(Sub Query)

`SQL`을 직접 사용하거나 `MyBatis` 등을 사용하는 레거시에서 많이 볼 수 있는 서브 쿼리는 실제로 `join`을 사용하거나 쿼리를 나눠서 처리하는 것이 성능상 유리할 때가 있습니다. 하지만 불가피하게
사용해야 할 상황들을 위해 `Querydsl`에서의 서브 쿼리 사용 방법을 확인해 보겠습니다.

서브 쿼리 사용을 위해선 `JPAExpressions`를 사용합니다.

먼저 선수들 중 나이가 가장 많은 선수를 서브 쿼리를 이용해 조회해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void simpleQuerydslWithSubQuery() {
        QPlayer subPlayer = new QPlayer("subPlayer"); // (1)

        Player founded = queryFactory
                .selectFrom(player)
                .where(player.age.eq(
                        JPAExpressions // (2)
                                .select(subPlayer.age.max())
                                .from(subPlayer)))
                .fetchOne();

        assertNotNull(founded);
        assertEquals(founded.getName(), "Kevin De Bruyne");
    }
}
```

> (1) 서로 다른 `alias`를 사용해야 하기 때문에 `QPlayer` 객체를 하나 더 생성합니다.  
> (2) `JPAExpressions`를 이용해 `where`절 내부에 또 하나의 쿼리를 생성합니다.

제가 생성한 네 명의 선수 중 `Kevin De Bruyne`이 나이가 가장 많기 때문에 테스트는 성공했고 로그를 확인해보면,

```text
2021-07-21 09:52:49.574 DEBUG 21302 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.age = (
            select
                max(subPlayer.age) 
            from
                Player subPlayer
        ) */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.age=(
                select
                    max(player1_.age) 
                from
                    player player1_
            )
```

이렇게 서로 다른 `alias` 두 개가 나타나는 것과, 서브 쿼리가 정확히 생성된 걸 확인할 수 있습니다.

이번엔 평균 나이보다 크거나 같은 선수들을 조회하는 테스트를 해보겠습니다.

> 이제부터는 중복되는 소스 코드는 생략하겠습니다.

```java
@Test
void simpleQuerydslWithSubQuery2() {
    QPlayer subPlayer = new QPlayer("subPlayer"); // (1)

    List<Player> players = queryFactory
            .selectFrom(player)
            .where(player.age.goe( // (2)
                    JPAExpressions
                            .select(subPlayer.age.avg()) // (3)
                            .from(subPlayer)))
            .fetch();

    assertNotNull(players);
    assertEquals(2, players.size());
}
```

> (1) 위에서와 마찬가지로 추가 `alias` 사용을 위해 객체를 생성합니다.  
> (2) 크거나 같은 값을 구하기 위해 `goe`(Greater than Or Equal to) 연산을 사용합니다.  
> (3) 평균 값을 구하기 위해 `avg` 함수를 사용합니다.

테스트 수행 결과 정상적으로 통과되었고(평균 나이가 28 이므로 그 이상인 손흥민, 케빈 데 브라위너 선수만 조회됨) 로그를 확인해보면,

```text
2021-07-21 09:58:50.926 DEBUG 21353 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.age >= (
            select
                avg(subPlayer.age) 
            from
                Player subPlayer
        ) */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.age>=(
                select
                    avg(cast(player1_.age as double)) 
                from
                    player player1_
            )
```

쿼리도 정상적으로 작성된 것을 확인할 수 있습니다.

이번엔 서브 쿼리를 활용하여 `where` 절의 `in` 안쪽을 채워 쿼리를 수행하는 테스트를 해보겠습니다.

나이가 29세 이하인 선수들의 나이를 찾아 `in` 절에 넣어 쿼리를 수행하도록 하였는데 사실 이는 매우 비표율적인 쿼리이지만 예제를 위해 사용한 것이므로 절대 이렇게 사용하시면 안 됩니다.

현재 구조 및 상태라면 `where` 절에서 `age` 관련 비교만 넣어도 동일한 결과를 얻을 수 있으니까요.

그냥 예시를 통해 문법 위주로 파악하시면 됩니다.

```java
@Test
void simpleQuerydslWithSubQuery3() {
    QPlayer subPlayer = new QPlayer("subPlayer");

    List<Player> players = queryFactory
            .selectFrom(player)
            .where(player.age.in(
                    JPAExpressions
                            .select(subPlayer.age)
                            .from(subPlayer)
                            .where(subPlayer.age.lt(29))))
            .fetch();

    assertNotNull(players);
    assertEquals(2, players.size());
}
```

테스트는 역시 잘 통과되었고 생성된 쿼리는 아래와 같습니다.

```text
    /* select
        player 
    from
        Player player 
    where
        player.age in (
            select
                subPlayer.age 
            from
                Player subPlayer 
            where
                subPlayer.age < ?1
        ) */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.age in (
                select
                    player1_.age 
                from
                    player player1_ 
                where
                    player1_.age<?
            )
```

이번엔 `select` 절에 서브 쿼리를 사용해보겠습니다.

역시 예제이다 보니 의미있는 쿼리는 아니라서 검증하는 것 보다 원하는 결과를 잘 가져왔는지 확인하는 것이 더 나을 거 같아 `assert` 대신 출력을 사용하였습니다. 

```java
@Test
void simpleQuerydslWithSubQuery4() {
    QPlayer subPlayer = new QPlayer("subPlayer");

    List<Tuple> players = queryFactory
            .select(player.name, JPAExpressions // (1)
                    .select(subPlayer.age.avg())
                    .from(subPlayer))
            .from(player)
            .fetch();

    System.out.println("players = " + players);
}
```

> (1) `select`절 안 쪽에 서브 쿼리를 사용하였습니다.

테스트 로그를 확인해보면,

```text
2021-07-21 11:21:02.102 DEBUG 21627 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        (select
            avg(subPlauer.age) 
        from
            Player subPlauer) 
    from
        Player player */ select
            player0_.name as col_0_0_,
            (select
                avg(cast(player1_.age as double)) 
            from
                player player1_) as col_1_0_ 
        from
            player player0_
players = [[Harry Kane, 28.0], [Heungmin Son, 28.0], [Kevin De Bruyne, 28.0], [Raheem Shaquille Sterling, 28.0]]
```

정확하게 쿼리가 생성된 것을 확인할 수 있습니다.

### 서브 쿼리 한계

`JPA`에서는 `from` 절의 서브 쿼리를 지원하지 않습니다. `JPA`를 활용하는 `Querydsl` 또한 당연히 지원할 수 없곘죠.

`JPA`를 사용하면서 `Hibernate` 구현체를 채택해서 사용하는 경우에 한해 `select`, `where` 절에 서브 쿼리를 사용할 수 있고 `Querydsl`에도 동일하게 적용됩니다.

`from` 절의 서브 쿼리 사용을 위해서는 맨 위에 언급했던 것 처럼 `join`으로 대체하거나 쿼리를 분리하거나 아니면 `Querydsl` 사용을 포기하고 `native SQL`을 사용하시면 됩니다.

최근 프로젝트를 하면서 느낀 점은, 좋은 회사일 수록 `DB`와 애플리케이션의 역할을 분명히 분리하려고합니다.

안 좋은 회사라고 표현할 수는 없지만 예전 기술이 아직까지 남아있거나 자체 솔루션에 애정이 크지 않은 일부 프리랜서(프리랜서를 비하하는 말은 절대 아닙니다) 개발자들을 급하게 수주해서 개발하였을 경우 DB 내에서 비즈니스 로직까지 처리해버리는 경우를 많이 목격했습니다.

이렇게되면 리팩터링이 정말 힘들어지고 할 수 있더라고 DB 테이블 스키마를 변경해야해서 서비스에 영향을 줄 확률이 높아진다든지 기존에 개발했던 내용을 싹 다 갈아엎어야 한다든지 여러 가지 부작용이 함께 발생할 수 있습니다.

물론 서브 쿼리를 이런 상황에서만 사용하는 것은 아닙니다만 대체할 수 있는 방법이 이미 충분히 있고, 서브 쿼리를 사용해야만 해결할 수 있는 상황이 있다면 설계 자체를 잘못되었을 확률도 있습니다.

서버에 부하를 주지 않기 위해 DB에서 처리하는 케이스도 물론 있을 수 있으나 되도록이면 이런 방법은 지양하는 것이 좋습니다.

이러한 내용은 [SQL AntiPatterns](http://www.yes24.com/Product/Goods/5269099?OzSrank=1)라는 책에도 잘 나와있으니 "왜 사용하면 안 되는 거야?" 하고 의심이 드는 분들은 꼭 구매해서 읽어보시기를 권장드립니다.

### Case (when, then)

서브 쿼리와 마찬가지로 JPA에서 지원하는 내용과 동일하게 지원하고, `select`, `where` 절에서 사용 가능합니다.

단순한 조건은 `when()`, `then()` 메서드 호출로 간단하게 구현할 수 있고 복잡할 경우 `CaseBuilder`를 사용합니다.

먼저 이름으로 국적을 획득할 수 있는 쿼리를 작성해보겠습니다.

```java
@Test
void simpleQuerydslWithSimpleCase() {
    List<String> nations = queryFactory
            .select(player.name
                    .when("Heungmin Son").then("대한민국") // (1)
                    .when("Harry Kane").then("잉글랜드")
                    .otherwise("기타")) // (2)
            .from(player)
            .fetch();
    nations.forEach(System.out::println);
}
```

> (1) `when()`, `then()`을 이용해 어떤 값일 때 어떤 값으로 대체할 지 작성합니다.  
> (2) 나머지 케이스에 대해 기본 값을 설정합니다.

테스트 결과는 아래와 같습니다.

```text
2021-07-21 12:09:49.362 DEBUG 22087 --- [           main] org.hibernate.SQL                        : 
    /* select
        case 
            when player.name = ?1 then ?2 
            when player.name = ?3 then ?4 
            else '기타' 
        end 
    from
        Player player */ select
            case 
                when player0_.name=? then ? 
                when player0_.name=? then ? 
                else '기타' 
            end as col_0_0_ 
        from
            player player0_
잉글랜드
대한민국
기타
기타
```

역시나 `SQL`과 결과가 예상한대로 정확하게 생성 및 출력되었습니다.

이제 조금 복잡한 케이스일 때 `CaseBuilder`를 이용해 `Predicate` 인터페이스를 넘겨주는 방식을 확인해보겠습니다.

```java
@Test
void simpleQuerydslWithComplexCase() {
    List<String> nations = queryFactory
            .select(new CaseBuilder() // (1)
                    .when(player.name.like("%Son")).then("대한민국") // (2)
                    .otherwise("기타")
            )
            .from(player)
            .fetch();
    nations.forEach(System.out::println);
}
```

> (1) CaseBuilder 객체를 생성합니다.  
> (2) `when` 메서드의 파라미터로 `Predicate`를 넘겨줍니다. `Q Type`에서 연산자가 반환하는 타입이 `Predicate`이기 때문에 복잡한 조건이 있더라도 쉽게 구현 가능합니다.

```text
2021-07-21 12:16:46.701 DEBUG 22135 --- [           main] org.hibernate.SQL                        : 
    /* select
        case 
            when (player.name like ?1 escape '!') then ?2 
            else '기타' 
        end 
    from
        Player player */ select
            case 
                when player0_.name like ? escape '!' then ? 
                else '기타' 
            end as col_0_0_ 
        from
            player player0_
기타
대한민국
기타
기타
```

테스트 결과 정상 동작 확인하였습니다.

⚠️ 서브 쿼리와 마찬가지로 되도록이면 `DB`에서 데이터를 가공하지 말고 애플리케이션 레이어에서 가공해야 `DB`와의 결합도를 줄일 수 있습니다.

### 상수

상수가 필요할 땐 `Expressions.constant()`를 사용합니다.

워낙 간단하기 때문에 설명 대신 테스트 소스 코드와 결과로 대체하겠습니다.

```java
@Test
void simpleQuerydslWithConstants() {
    List<Tuple> age = queryFactory
            .select(player.name, Expressions.constant("NAME")) // (1)
            .from(player)
            .fetch();
    age.forEach(System.out::println);
}
```
> (1) `Expressions.constant()` 사용

```text
2021-07-21 12:26:18.647 DEBUG 22232 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name 
    from
        Player player */ select
            player0_.name as col_0_0_ 
        from
            player player0_
[Harry Kane, NAME]
[Heungmin Son, NAME]
[Kevin De Bruyne, NAME]
[Raheem Shaquille Sterling, NAME]
```

### concat

`select`한 결과에 문자열을 더해줘야 할 때 사용합니다.

이름: 나이로 출력하기 위한 테스트를 작성하였습니다.

```java
@Test
void simpleQuerydslWithConcat() {
    List<String> nameWithAge = queryFactory
        .select(player.name.concat(": ").concat(player.age.stringValue()))
        .from(player)
        .orderBy(player.age.desc())
    .fetch();
    nameWithAge.forEach(System.out::println);
}
```
> (1) `concat()`은 `String` 타입만 파라미터로 전달할 수 있기 때문에 `player.age`처럼 다른 타입의 경우 `stringValue()`를 호출해줘야 합니다.

```text
2021-07-21 12:30:44.676 DEBUG 22281 --- [           main] org.hibernate.SQL                        : 
    /* select
        concat(concat(player.name,
        ?1),
        str(player.age)) 
    from
        Player player 
    order by
        player.age desc */ select
            ((player0_.name||?)||cast(player0_.age as char)) as col_0_0_ 
        from
            player player0_ 
        order by
            player0_.age desc
Kevin De Bruyne: 30
Heungmin Son: 29
Harry Kane: 27
Raheem Shaquille Sterling: 26
```

원하는대로 결과가 잘 출력되는 것을 확인할 수 있습니다.

---

오늘 정리한 내용들은 모두 애플리케이션 레이어에서도 충분히 다룰 수 있는 것들이라 부득이한 경우가 아니라면 사용하지 않는 것이 좋습니다.

`DB`와 애플리케이션 레이어가 잘 분리될 수록 확장에 용이하고 유지보수에도 도움이 되기 때문입니다.

다음 포스팅에서는 `Projection`에 대해 다뤄보겠습니다.
