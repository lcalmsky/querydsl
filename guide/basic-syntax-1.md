![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

Querydsl의 기본 문법을 소개합니다.

### Q Type

> 이전 포스팅에서 사용한 `Entity`들을 `compileQuerydsl`을 이용해 모두 `Q Type`으로 변환하였습니다. (자세한 내용은 [이전 포스팅](https://jaime-note.tistory.com/67) 참고)

`Q Type` 객체를 사용하는 방법은 이전 포스팅에서도 소개했지만 두 가지가 있습니다.

```java
QPlayer player = new QPlayer("p"); // (1)
QPlayer player = Qplayer.player; // (2)
```

> (1) `alias`를 별도로 지정하고 `new`를 이용해 객체를 생성합니다.  
> (2) `QPlayer`클래스에 `static`으로 선언된 객체를 가져와 사용합니다. `alias`의 기본 값은 `Entity` 이름 입니다. (Player인 경우 player)  

굳이 `new`로 객체를 생성할 필요 없이 `Qplayer` 자체를 `static import`하여 사용하면 깔끔하게 사용할 수 있습니다.

간단한 테스트 코드를 작성하여 `QPlayer`를 `static import`하여 사용한 모습입니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;;

import static io.lcalmsky.querydsl.domain.QPlayer.player; // (1)
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void simpleQuerydslTest() {
        // when
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Player founded = queryFactory.select(player) // (2)
                .from(player)
                .where(player.name.like("%Son"))
                .fetchOne();
        // then
        assertNotNull(founded);
        assertEquals("Heungmin Son", founded.getName());
    }
}
```

> (1) `static`으로 `QPlayer.player`를 `import` 합니다.  
> (2) 아주 분명하고 명쾌한 변수 이름으로 사용할 수 있습니다. 

테스트를 실행한 뒤 `SQL`이 아닌 `JPQL`을 로그로 확인하고 싶다면 `application.yml`에 아래 설정을 추가해줍니다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true # (1)
        use_sql_comments: true # (2)
logging:
  level:
    org.hibernate.SQL: debug # (3)
```

> (1) `SQL`을 정렬된 형태의 로그로 출력합니다.  
> (2) `JPQL`을 로그로 출력합니다.  
> (3) `org.hibernate.SQL`의 로그 레벨을 `debug`로 지정해야 로그가 출력됩니다.

테스트를 실행해서 로그를 확인해볼까요?

```text
2021-07-16 23:02:41.075 DEBUG 12643 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name like ?1 escape '!' */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.name like ? escape '!'
```
`JPQL`이 먼저 comment (`/* */`)로 출력되고 그 이후에 `SQL`이 출력되는 것을 확인할 수 있습니다.

개인적으로 오히려 가독성을 해치는 거 같아서 이 옵션을 자주 사용하진 않습니다.

`static import`가 아닌 `new` 객체를 이용해 `alias`를 다르게 지정해보겠습니다.

```java
@Test
void simpleQuerydslWithAliasTest() {
    // when
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    QPlayer player = new QPlayer("p"); // (1)
    Player founded = queryFactory.select(player)
            .from(player)
            .where(player.name.like("%Son"))
            .fetchOne();
    // then
    assertNotNull(founded);
    assertEquals("Heungmin Son", founded.getName());
}
```

> (1) `alias`를 `p`로 지정하였습니다.

```text
2021-07-16 23:07:42.520 DEBUG 12698 --- [           main] org.hibernate.SQL                        : 
    /* select
        p 
    from
        Player p 
    where
        p.name like ?1 escape '!' */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ 
        where
            player0_.name like ? escape '!'
```

`alias`가 `p`로 바뀐 것을 확인할 수 있습니다.

이 기능은 같은 테이블을 `join` 해야하는 경우에만 사용합니다.

기본적으로 `static import`를 사용하시는 것을 권장합니다. ☺️

### 조건절 (where clause)

이미 위에 테스트코드에의해 스포일러 당한 부분이 없잖아 있지만 좀 더 자세하게 살펴보겠습니다.

```java
queryFactory.select(player).from(player);
queryFactory.selectFrom(player);
```

우선 `select`, `from`의 파라미터가 같은 경우 `selectFrom`으로 합칠 수 있습니다.

테스트 코드를 먼저 살펴보면,

```java
@Test
void simpleQuerydslWithWhereClauseTest() {
    // given
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    Player founded = queryFactory.selectFrom(player)
        .where(player.name.like("%Son")
            .and(player.age.lt(30))
            .and(player.team.name.ne("Manchester City F.C.")))
        .fetchOne();
    // then
    assertNotNull(founded);
    assertEquals("Heungmin Son", founded.getName());
}
```

조건절인 `where` 부분을 살펴보면 SQL을 작성하듯이 편리하게 작성할 수 있습니다.

equals(==, `eq`), not equals(!=, `ne`), `like`, less than(<, `lt`) 등 `SQL`로 표현할 수 있는 연산자들을 영어의 축약형으로 사용하고, `and`, `or` 등 조건 추가도 `method chaining` 형태로 쉽게 가능합니다. 

`and` 조건을 사용하는 경우 `method chaning` 방식대신 `콤마(,)`를 이용해 파라미터를 분리하여 작성해도 되는데 이 방식은 동적 쿼리를 작성할 때 매우 편리합니다.

```java
Player founded = queryFactory.selectFrom(player)
    .where(player.name.like("%Son"),
        player.age.lt(30),
        player.team.name.ne("Manchester City F.C."))
    .fetchOne();
```

테스트를 실행해서 쿼리를 확인해보면,

```text
2021-07-16 23:21:09.697 DEBUG 12812 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.name like ?1 escape '!' 
        and player.age < ?2 
        and player.team.name <> ?3 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.name as name3_1_,
            player0_.team_id as team_id4_1_ 
        from
            player player0_ cross 
        join
            team team1_ 
        where
            player0_.team_id=team1_.team_id 
            and (
                player0_.name like ? escape '!'
            ) 
            and player0_.age<? 
            and team1_.name<>?
```

의도한대로 잘 표현된 것을 확인할 수 있습니다.

제공하는 검색 조건은 JPQL과 동일하고 아래 처럼 표현할 수 있습니다.

* `eq("something")`: = 'something'
* `ne("something")`: != 'something'
* `eq("something").not()`: != 'something'
* `like("%something")`: like '%something'
* `startsWith("something")`: like 'something%'
* `contains("something")`: like '%something%'
* `isNull()`: is null
* `isNotNull()`: is not null
* `isEmpty()`: 길이가 0
* `isNotEmpty()`: 길이가 0이 아님
* `in("foo", "bar")`: in("foo", "bar")
* `notIn("foo", "bar")`: not in("foo", "bar")
* `in("foo", "bar").not()`: not in("foo", "bar")
* `between(20, 30)`: between 20, 30
* `notBetween(20, 30)`: not between 20, 30
* `between(20, 30).not()`: not between 20, 30 
* `gt(28)`: > 28
* `goe(28)`: >= 28 
* `lt(28)`: < 28
* `loe(28)`: <= 28

### 결과 매핑

Querydsl은 결과를 매핑하는 방법 역시 여러 가지로 제공합니다.

* `fetch()`: 리스트 반환, 결과가 없는 경우 빈 리스트 반환
* `fetchOne()`: 한 건 조회
    * 결과가 없는 경우: `null` 반환
    * 결과가 여러 개인 경우: `NonUniqueResultException` 발생
* `fetchFirst()`: 처음 한 건 조회
    * `limit(1).fetch()`와 동일
* `fetchResults()`: 결과에 페이지 정보 포함, `total count` 쿼리 추가 수행
    * `total count` 쿼리는 `count(id)` 사용
* `fetchCount()`: `count` 쿼리 수행

---

다음 포스팅에서는 페이징과 정렬, 집합을 사용하는 방식을 다뤄보겠습니다.