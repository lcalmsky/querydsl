![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

[이전 포스팅](https://jaime-note.tistory.com/74)에 이어서 `Querydsl`의 중급 문법을 소개합니다.

### Projection

프로젝션(Projection)은 select 절에서 어떤 컬럼들을 조회할지 대상을 지정하는 것을 말합니다.

프로젝션 대상이 하나일 경우는 타입이 명확하기 때문에 해당 `Generic Type`이 해당 컬럼 타입에 맞게 지정됩니다.

이 부분은 너무 간단하기 때문에 간단한 예제 소스 코드로 설명을 대체하겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
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
    void simpleQuerydslWithOnlyOneProjection() {
        List<String> playerNames = queryFactory.select(player.name) // (1)
                .from(player)
                .fetch();
        playerNames.forEach(System.out::println);
    }
}
```

> (1) `player entity`의 `name` 필드를 지정하면 `name`의 `Type`인 `String`으로 결과를 반환합니다.

```text
2021-07-22 11:34:39.099 DEBUG 27771 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name 
    from
        Player player */ select
            player0_.name as col_0_0_ 
        from
            player player0_
Harry Kane
Heungmin Son
Kevin De Bruyne
Raheem Shaquille Sterling
```

실행 결과 원하는대로 이름만 가져온 것을 확인할 수 있습니다.

프로젝션 대상이 둘 이상이면 명확한 타입을 지정할 수 없기 때문에 `Tuple`이나 특정 클래스로 반환할 수 있습니다.

```java
@Test
void simpleQuerydslWithMultiProjection() {
    List<Tuple> tuples = queryFactory.select(player.name, player.age)
        .from(player)
        .fetch();
    tuples.forEach(tuple -> System.out.printf("%s: %d%n", tuple.get(player.name), tuple.get(player.age)));
}
```

> (1) `select` 안에서 두 개 이상의 필드를 지정하였기 때문에 반환타입을 `Tuple`로 지정하였습니다.

```text
2021-07-22 15:37:23.113 DEBUG 28812 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
Harry Kane: 27
Heungmin Son: 29
Kevin De Bruyne: 30
Raheem Shaquille Sterling: 26
```

`Tuple` 자체를 출력하게 되면 배열 형태로 출력이 되고, `Tuple` 내에서 데이터에 접근하기 위해선 `get`메서드의 파라미터로 `Q Type`의 필드를 넘겨주면 됩니다.

`Tuple` 자체를 응답규격으로 사용하게 되면 `JPA`를 쓰면서 `Entity`를 그대로 반환하는 것과 마찬가지로 외부에 DB를 그대로 노출할 수 있으므로 되도록이면 다른 객체로 매핑하여 반환하는 것이 좋습니다.

### 클래스 매핑

프로젝션을 사용해 조회해 온 결과를 커스텀 클래스에 매핑하는 방법을 소개합니다.

기존 JPA에서 JPQL을 이용한 방식([@Query로 바로 클래스에 매핑하기](https://jaime-note.tistory.com/51?category=849450) 참조)은 생성자를 통해서만 가능했었는데 `Querydsl`을 이용하면 훨신 더 깔끔한 방법으로 해결 가능합니다.

조회 결과를 클래스에 매핑하기 위해 세 가지 접근 방법을 제공합니다.

* Property: setter 사용, 기본 생성자 사용
* Field: setter 필요 없음, 기본 생성자 필요 없음, 필드와 매핑 
* Constructor: @AllArgsConstructor 필요, setter 필요 없음 

먼저 Property를 이용한 방법을 살펴보겠습니다.

먼저 매핑시킬 클래스를 생성했습니다.

```java
package io.lcalmsky.querydsl.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerDefaultData {
    private String name;
    private int age;
}
```

단순하게 이름, 나이를 가지는 클래스로 `@Data`를 사용하였기 때문에 `getter/setter/toString`을 외부에서 사용할 수 있습니다.

```java
@Test
void simpleQuerydslWithProjectionUsingProperties() {
    List<PlayerDefaultData> players = queryFactory
        .select(Projections.bean(PlayerDefaultData.class, player.name, player.age))
        .from(player)
        .fetch();
    players.forEach(System.out::println);
}
```

> (1) `com.querydsl.core.types.Projections`의 메서드인 `bean()`을 호출하여 매핑할 클래스와 매핑할 필드를 순서대로 전달합니다.

실행한 결과는 아래와 같습니다.

```text
2021-07-22 21:41:55.873 DEBUG 3369 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
PlayerDefaultData(name=Harry Kane, age=27)
PlayerDefaultData(name=Heungmin Son, age=29)
PlayerDefaultData(name=Kevin De Bruyne, age=30)
PlayerDefaultData(name=Raheem Shaquille Sterling, age=26)
```

`PlayerDefaultData`의 `List`가 정확하게 출력된 것을 확인할 수 있습니다.

`bean` 대신 `fields`를 사용하여도 동일한 결과를 얻을 수 있습니다.

```java
@Test
void simpleQuerydslWithProjectionUsingFields() {
    List<PlayerDefaultData> players = queryFactory
            .select(Projections.fields(PlayerDefaultData.class, player.name, player.age)) // (1)
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

> (1) `fields()` 메서드를 호출하여 매핑합니다.

`bean()`과 `fields()`의 차이점은 `fields()`의 경우 `getter/setter`가 필요 없다는 점인데요, `PlayerDefaultData`에서 @Data 애너테이션을 제거한 뒤 테스트해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

public class PlayerDefaultData {
    private String name;
    private int age;
}
```

```text
2021-07-22 21:45:51.997 DEBUG 3400 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
io.lcalmsky.querydsl.domain.PlayerDefaultData@10660795
io.lcalmsky.querydsl.domain.PlayerDefaultData@3ccc4ca0
io.lcalmsky.querydsl.domain.PlayerDefaultData@1dbfbd94
io.lcalmsky.querydsl.domain.PlayerDefaultData@ec1b776
```

@Data를 없앴기 때문에 toString()이 제대로 동작하지 않았으나 어쨌든 제대로 조회해오는 것을 확인할 수 있습니다.

마지막으로 생성자를 이용한 방식입니다.

일일히 적기 귀찮아서 `@AllArgsConstructor` 애너테이션을 사용했습니다.

```java
package io.lcalmsky.querydsl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
public class PlayerDefaultData {
    private String name;
    private int age;
}
```

```java
@Test
void simpleQuerydslWithProjectionUsingConstructor() {
    List<PlayerDefaultData> players = queryFactory
            .select(Projections.constructor(PlayerDefaultData.class, player.name, player.age)) // (1)
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

> (1) `fields()` 대신 `constructor()`를 사용합니다.

```text
2021-07-22 21:49:06.857 DEBUG 3444 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
io.lcalmsky.querydsl.domain.PlayerDefaultData@a36ff0b
io.lcalmsky.querydsl.domain.PlayerDefaultData@1ecf784f
io.lcalmsky.querydsl.domain.PlayerDefaultData@344769b9
io.lcalmsky.querydsl.domain.PlayerDefaultData@34376069
```

마찬가지로 정상적으로 조회해오는 것을 확인할 수 있습니다.

개인적으로 `DTO`(Data Transfer Object)나 `VO`(Value Object)에는 거의 무조건 `getter/setter`를 사용하기 때문에 굳이 복잡한 생성자를 만들 필요 없이 `bean()`을 이용해 생성하는 것을 권장합니다.

세 가지 모두 사용할 가능성을 열어두고 싶으시다면

```java
package io.lcalmsky.querydsl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerDefaultData {
    private String name;
    private int age;
}
```

이렇게 모두 사용하시면 됩니다.

`PlayerDefaultData`에서는 `Player Entity`와 동일한 필드명을 사용했는데, 필드명이 다를 경우 다시 이 세 가지를 구분해서 사용해야 합니다.

먼저 PlayerData라는 클래스를만들어 필드명을 다르게 해보겠습니다.

```java
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
```

이렇게 `name`을 `playerName`으로, `age`를 `playerAge`로 변경하였습니다.

그리고나서 `bean()`을 이용해 테스트를 실행해보면

```java
@Test
void simpleQuerydslWithProjectionUsingBeanWithDifferentFieldNames() {
    List<PlayerData> players = queryFactory
        .select(Projections.bean(PlayerData.class, player.name, player.age))
        .from(player)
        .fetch();
    players.forEach(System.out::println);
}
```

```text
// 쿼리 생략
PlayerData(playerName=null, playerAge=0)
PlayerData(playerName=null, playerAge=0)
PlayerData(playerName=null, playerAge=0)
PlayerData(playerName=null, playerAge=0)
```

이렇게 제대로 매핑되지 않습니다. `setter`를 이용하는데 `setter` 이름이 `setPlayerName`, `setPlayerAge`로 생성될 것이기 때문입니다.

그리고 `fields` 역시 마찬가지입니다. `field`의 이름으로 매핑해야하는데 이름이 다르기 때문이죠.

```java
@Test
void simpleQuerydslWithProjectionUsingFieldsWithDifferentFieldNames() {
    List<PlayerData> players = queryFactory
            .select(Projections.fields(PlayerData.class, player.name, player.age))
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

수행한 결과는 위와 같아 생략합니다.

반면 `constructor`는 각각 위치에 타입만 일치한다면 정확하게 매핑해줍니다.

```java
@Test
void simpleQuerydslWithProjectionUsingConstructorWithDifferentFieldNames() {
    List<PlayerData> players = queryFactory
            .select(Projections.constructor(PlayerData.class, player.name, player.age))
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

```text
PlayerData(playerName=Harry Kane, playerAge=27)
PlayerData(playerName=Heungmin Son, playerAge=29)
PlayerData(playerName=Kevin De Bruyne, playerAge=30)
PlayerData(playerName=Raheem Shaquille Sterling, playerAge=26)
```

이제 세 가지의 차이 점이 좀 더 명확해 보입니다.

그럼 무조건 `constructor`를 사용해야할까요?

당연히 아니죠.

`as()`를 이용해 매핑할 필드 이름을 변경해주시면 됩니다.

```java
@Test
void simpleQuerydslWithProjectionUsingBeanWithDifferentFieldNames() {
    List<PlayerData> players = queryFactory
            .select(Projections.bean(PlayerData.class, player.name.as("playerName"), player.age.as("playerAge")))
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

이렇게 as()를 이용해 필드명을 변경해주면,

```text
2021-07-22 22:07:27.918 DEBUG 3630 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name as playerName,
        player.age as playerAge 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
PlayerData(playerName=Harry Kane, playerAge=27)
PlayerData(playerName=Heungmin Son, playerAge=29)
PlayerData(playerName=Kevin De Bruyne, playerAge=30)
PlayerData(playerName=Raheem Shaquille Sterling, playerAge=26)
```

쿼리에서 `as`를 이용해 매핑될 필드명을 사용한 것을 확인할 수 있고 결과도 정확하게 출력됩니다.

서브 쿼리에서도 ExpressionUtils.as()를 이용해 조회한 결과를 매핑할 필드의 `alias`를 지정할 수 있습니다만 서브 쿼리를 자주 사용할 일 자체가 없는 것이 바람직하기 때문에 설명을 생략하겠습니다.

필드도 동일하게 ExpressionUtils.as()를 사용할 수 있지만 필드명 뒤에 as()를 사용하는 것이 더 직관적이라 굳이 사용할 필요는 없을 거 같습니다.

```java
queryFactory
    .select(Projections.bean(PlayerData.class,
            ExpressionUtils.as(player.name, "playerName"), // (1)
            player.age.as("playerAge"))) // (2)
    .from(player)
    .fetch();
```

> (1) ExpressionUtils.as를 사용해 필드명을 매핑  
> (2) as를 사용해 필드명을 매핑

진짜 마지막으로 `join`을 이용해 가져온 필드를 매핑해보고 마무리하도록 하겠습니다.

먼저 `PlayerWithTeamData`라는 팀 이름을 추가 속성으로 가지는 클래스를 생성해보겠습니다.

```java
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

```

그리고 `join`을 이용해 조회해온 뒤 매핑해보도록 하겠습니다.

```java
@Test
void simpleQuerydslWithProjectionUsingJoin() {
    List<PlayerWithTeamData> players = queryFactory
            .select(Projections.bean(PlayerWithTeamData.class, player.name, player.age, player.team.name.as("teamName")))
            .from(player)
            .join(player.team, team)
            .fetch();
    players.forEach(System.out::println);
}
```

`bean()`을 사용했으므로 `setter`를 사용해 매핑했고 `teamName`에 매핑될 수 있게 `as()`로 `alias`를 추가해줬습니다.

테스트 결과는

```text
2021-07-22 22:23:38.773 DEBUG 3778 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.team.name as teamName 
    from
        Player player   
    inner join
        player.team as team */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            team1_.name as col_2_0_ 
        from
            player player0_ 
        inner join
            team team1_ 
                on player0_.team_id=team1_.team_id
PlayerWithTeamData(name=Harry Kane, age=27, teamName=Tottenham Hotspur F.C.)
PlayerWithTeamData(name=Heungmin Son, age=29, teamName=Tottenham Hotspur F.C.)
PlayerWithTeamData(name=Kevin De Bruyne, age=30, teamName=Manchester City F.C.)
PlayerWithTeamData(name=Raheem Shaquille Sterling, age=26, teamName=Manchester City F.C.)
```

이렇게 정상적으로 매핑되어 출력되는 것을 확인할 수 있습니다.

### @QueryProjection

다음으로 생성자와 `@QueryProjection` 애너테이션을 활용해서 클래스에 매핑해보겠습니다.

먼저 PlayerDefaultData 클래스를 변경해줍니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PlayerDefaultData {
    private String name;
    private int age;

    @QueryProjection
    public PlayerDefaultData(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

그리고 `Q Type`을 생성할 때 사용했던 `gradle` 명령어 `compileQuerydsl`을 실행해줍니다.

> 이 부분에 대한 자세한 설명은 [이 포스팅](https://jaime-note.tistory.com/67?category=994945)을 참고하세요 😀

```shell
> ./gradlew compileQuerydsl
```

실행하고나면 `QPlayerDefaultData` 클래스가 지정한 경로에 생성됩니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.Generated;

@Generated("com.querydsl.codegen.ProjectionSerializer")
public class QPlayerDefaultData extends ConstructorExpression<PlayerDefaultData> {

    private static final long serialVersionUID = 1954213705L;

    public QPlayerDefaultData(com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<Integer> age) {
        super(PlayerDefaultData.class, new Class<?>[]{String.class, int.class}, name, age);
    }

}
```

이 클래스는 `Projections`을 사용했던 자리에 `new`를 사용해 객체를 생성하는 것으로 대체할 수 있게 해줍니다.

```java
@Test
void simpleQuerydslWithProjectionUsingQueryProjection() {
    List<PlayerDefaultData> players = queryFactory
            .select(new QPlayerDefaultData(player.name, player.age)) // (1)
            .from(player)
            .fetch();
    players.forEach(System.out::println);
}
```

> (1) `QPlayerDefaultData` 객체를 생성하면서 생성자에 필드를 넘겨줍니다.

이제 실행해보면,

```text
2021-07-22 22:45:51.468 DEBUG 4005 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age 
    from
        Player player */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_ 
        from
            player player0_
PlayerDefaultData(name=Harry Kane, age=27)
PlayerDefaultData(name=Heungmin Son, age=29)
PlayerDefaultData(name=Kevin De Bruyne, age=30)
PlayerDefaultData(name=Raheem Shaquille Sterling, age=26)
```

이렇게 잘 매핑된 것을 확인할 수 있습니다.

`@QueryProjection`의 장점은 컴파일시에 파라미터가 잘못 전달되면 바로 에러를 발생시켜주는 것인데요, `bean`, `fields`, `constructor` 모두 첫 파라미터를 제외한 나머지 파라미터가 ...으로 표현되어 필드 갯수를 마음대로 전달할 수 있기 때문입니다.

반면에 데이터 전달을 위한 클래스가 `Querydsl`과 의존성을 가지기 때문에 규격이 바뀌든 `DataSource`가 바뀌든 다 영향을 받게 됩니다.

저는 개인적으로 객체지향 설계를 매우 중시하기 때문에 이렇게 서로 의존성이 깊어지는 코드를 지양하는 편입니다. 언젠간 하나의 작은 수정이 여러 군데 영향을 미칠 수 있기 때문입니다.

따라서 이런 기능을 사용할 때는 협업하는 개발자끼리 충분한 협의가 필요합니다.

---

다음 포스팅에서는 동적 쿼리에 대해 알아보겠습니다. 😁