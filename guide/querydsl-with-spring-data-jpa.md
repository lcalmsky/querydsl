![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

`스프링 데이터 JPA`와 `Querydsl`을 같이 사용하는 방법에 대해서 알아보겠습니다.

> 사실 이 부분은 `스프링 데이터 JPA` 관련 포스팅의 복습이라고 보셔도 됩니다.
>
> 각 챕터에 앞서 관련 내용을 미리 읽고 오시면 도움이 될 거 같아 본격적인 설명에 앞서 링크를 먼저 첨부하겠습니다.
>
> * [스프링 데이터 JPA - Custom Repository Best Practice](https://jaime-note.tistory.com/58?category=849450)
> * [스프링 데이터 JPA - 페이징과 정렬2(API 활용)](https://jaime-note.tistory.com/61?category=849450)

그럼 위의 내용을 이미 알고계시다는 전제 하에 시작해보겠습니다! 🏃‍

### 사용자 정의 Repository

먼저 `PlayerRepository`를 생성합니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByName(String name);
}
```

대부분 기본으로 제공하는 쿼리를 사용할 예정이므로 이름으로 선수를 조회하기위해 쿼리 메서드 하나만 추가해줬습니다.

`PlayerRepository`가 잘 동작하는지 확인하기 위해 테스트 코드를 작성했습니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PlayerRepositoryTest {
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setup() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        harryKane.contactSalary(200000);
        harryKane.begins();
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        heungminSon.contactSalary(140000);
        heungminSon.begins();
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        kevinDeBruyne.contactSalary(350000);
        kevinDeBruyne.begins();
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);
        raheemSterling.contactSalary(300000);
        raheemSterling.begins();

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
    }

    @Test
    void testBasicFunctions() {
        // when
        List<Player> players = playerRepository.findAll();
        // then
        assertEquals(4, players.size());
        // print
        players.forEach(System.out::println);

        // when
        List<Player> playerByName = playerRepository.findByName("Heungmin Son");
        // then
        assertEquals(1, playerByName.size());
        assertEquals("Heungmin Son", playerByName.get(0).getName());
        //print
        playerByName.forEach(System.out::println);
    }
}
```

기본 기능 중 하나인 `findAll()`과 쿼리 메서드로 추가한 `findByName()`을 테스트했고 모두 성공했습니다.

그냥 넘어가기 아쉬우니 로그를 한 번 봐볼까요?

잠시 후 동적 쿼리도 테스트 할 것이기 때문에 파라미터도 출력할 수 있도록 `application.yml` 파일에 로그 레벨을 추가해줬습니다.

```yaml
logging:
  level:
    org.hibernate:
      SQL: debug
      type.descriptor.sql: trace
```

```text
2021-07-25 01:02:21.780 DEBUG 4473 --- [           main] org.hibernate.SQL                        : 
    /* select
        generatedAlias0 
    from
        Player as generatedAlias0 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_
2021-07-25 01:02:21.785 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([player_i1_1_] : [BIGINT]) - [3]
2021-07-25 01:02:21.786 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([player_i1_1_] : [BIGINT]) - [4]
2021-07-25 01:02:21.786 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([player_i1_1_] : [BIGINT]) - [5]
2021-07-25 01:02:21.786 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([player_i1_1_] : [BIGINT]) - [6]
Player(id=3, name=Harry Kane, age=27, inSeason=true, weeklySalary=200000)
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000)
Player(id=5, name=Kevin De Bruyne, age=30, inSeason=true, weeklySalary=350000)
Player(id=6, name=Raheem Shaquille Sterling, age=26, inSeason=true, weeklySalary=300000)
2021-07-25 01:02:21.837 DEBUG 4473 --- [           main] org.hibernate.SQL                        : 
    /* select
        generatedAlias0 
    from
        Player as generatedAlias0 
    where
        generatedAlias0.name=:param0 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_ 
        where
            player0_.name=?
2021-07-25 01:02:21.838 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [Heungmin Son]
2021-07-25 01:02:21.838 TRACE 4473 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([player_i1_1_] : [BIGINT]) - [4]
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000)
```

처음엔 `findAll`로 모두 가져와서 결과를 출력하였고, 이후엔 `findByName`으로 손흥민선수만 조회하여 출력한 것을 확인할 수 있습니다.

정상 동작을 확인했으니 사용자 정의 `Repository`를 추가보겠습니다.

먼저 `CustomPlayerRepository`를 생성합니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;

import java.util.List;

public interface CustomPlayerRepository {
    List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam);
}
```

`findPlayerTeamBy`라는 메서드는 쿼리 파라미터(`PlayerQueryParam`)를 전달받아 `PlayerDetails` 라는 데이터 클래스로 매핑해 반환할 예정입니다.

두 클래스를 생성해볼까요?

```java
package io.lcalmsky.querydsl.domain.param;

import lombok.Data;

@Data
public class PlayerQueryParam {
    private String name;
    private Integer age;
    private String teamName;
}
```

이름, 나이, 팀 이름을 파라미터로 전달하기 위한 클래스 입니다.

```java
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
```

이름, 나이, 시즌 중 여부, 주급, 팀 이름을 반환하기 위핸 데이터 클래스 입니다.

이제 `PlayerRepository`가 `CustomPlayerRepository`를 상속하도록 수정해보겠습니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, CustomPlayerRepository {
    List<Player> findByName(String name);
}
```

다음은 `CustomPlayerRepository`의 구현체를 만들 차례입니다.

구현체의 `postfix`를 수정할 생각이 없기 때문에 `PlayerRepositoryImpl`로 생성하였습니다.

```java
package io.lcalmsky.querydsl.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;


public class PlayerRepositoryImpl implements CustomPlayerRepository {
    private final JPAQueryFactory queryFactory;

    public PlayerRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam) {
        return queryFactory
                .select(Projections.bean(PlayerDetails.class, player.name, player.age, player.inSeason, player.weeklySalary, team.name.as("teamName")))
                .from(player)
                .leftJoin(player.team, team)
                .where(condition(playerQueryParam.getName(), player.name::eq),
                        condition(playerQueryParam.getAge(), player.age::eq),
                        condition(playerQueryParam.getTeamName(), team.name::eq))
                .fetch();
    }

    private <T> BooleanExpression condition(T value, Function<T, BooleanExpression> function) {
        return Optional.ofNullable(value).map(function).orElse(null);
    }
}
```

`Projection`([이전 포스팅 참고](https://jaime-note.tistory.com/75))을 이용해 `select`에서 바로 `PlayerDetails` 객체를 생성하도록 하였고 `Player`, `Team` `Entity`를 모두 조회하기 위해 `join`을 사용하였습니다.

그리고 파라미터별로 존재하면 `where`절에 `and` 조건으로 사용될 수 있게 구현([이전 포스팅 참고](https://jaime-note.tistory.com/76))하였습니다.

이제 테스트를 만들어 잘 동작하는지 확인해보겠습니다.

```java
@Test
void dynamicQueryTest() {
    // given
    PlayerQueryParam playerQueryParam = new PlayerQueryParam();
    playerQueryParam.setName("Heungmin Son");
    playerQueryParam.setAge(29);
    playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

    // when
    List<PlayerDetails> players = playerRepository.findPlayerTeamBy(playerQueryParam);

    // then
    assertEquals(1, players.size());

    // print
    players.forEach(System.out::println);
}
```

파라미터로 손흥민선수의 이름, 나이, 팀 이름을 전달하였습니다.

테스트 결과,

```text
2021-07-25 01:15:52.611 DEBUG 4538 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        player.name = ?1 
        and player.age = ?2 
        and team.name = ?3 */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            player0_.name=? 
            and player0_.age=? 
            and team1_.name=?
2021-07-25 01:15:52.612 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [Heungmin Son]
2021-07-25 01:15:52.613 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [INTEGER] - [29]
2021-07-25 01:15:52.613 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [Tottenham Hotspur F.C.]
2021-07-25 01:15:52.620 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_0_0_] : [VARCHAR]) - [Heungmin Son]
2021-07-25 01:15:52.620 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_1_0_] : [INTEGER]) - [29]
2021-07-25 01:15:52.621 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_2_0_] : [BOOLEAN]) - [true]
2021-07-25 01:15:52.621 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_3_0_] : [INTEGER]) - [140000]
2021-07-25 01:15:52.621 TRACE 4538 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_4_0_] : [VARCHAR]) - [Tottenham Hotspur F.C.]
PlayerDetails(name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000, teamName=Tottenham Hotspur F.C.)
```

쿼리가 정상적으로 생성되었고 파라미터도 정확히 전달되었으며 테스트도 통과되었습니다.

테스트를 하나 추가하여 토트넘 소속 선수들만 조회해보겠습니다.

```java
@Test
void dynamicQueryTest2() {
    // given
    PlayerQueryParam playerQueryParam = new PlayerQueryParam();
    playerQueryParam.setTeamName("Tottenham Hotspur F.C."); // (1)

    // when
    List<PlayerDetails> players = playerRepository.findPlayerTeamBy(playerQueryParam);

    // then
    assertEquals(2, players.size()); // (2)

    // print
    players.forEach(System.out::println);
}
```

> (1) 파라미터에 팀 이름만 설정하였습니다.  
> (2) 토트넘 소속 선수는 두 명 등록되어있기 때문에 검증하기 위한 값을 수정해주었습니다.

```text
2021-07-25 01:34:23.133 DEBUG 4645 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=?
2021-07-25 01:34:23.134 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [Tottenham Hotspur F.C.]
2021-07-25 01:34:23.139 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_0_0_] : [VARCHAR]) - [Harry Kane]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_1_0_] : [INTEGER]) - [27]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_2_0_] : [BOOLEAN]) - [true]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_3_0_] : [INTEGER]) - [200000]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_4_0_] : [VARCHAR]) - [Tottenham Hotspur F.C.]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_0_0_] : [VARCHAR]) - [Heungmin Son]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_1_0_] : [INTEGER]) - [29]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_2_0_] : [BOOLEAN]) - [true]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_3_0_] : [INTEGER]) - [140000]
2021-07-25 01:34:23.140 TRACE 4645 --- [           main] o.h.type.descriptor.sql.BasicExtractor   : extracted value ([col_4_0_] : [VARCHAR]) - [Tottenham Hotspur F.C.]
PlayerDetails(name=Harry Kane, age=27, inSeason=true, weeklySalary=200000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000, teamName=Tottenham Hotspur F.C.)
```

역시 정상적으로 수행된 것을 확인할 수 있습니다.

특히 동적 쿼리를 이용했기 때문에 `where`절에 팀 관련 조건만 존재하는 것을 확인할 수 있습니다.

### 페이징

`스프링 데이터 JPA`에서 사용하는 `Pageable` 인터페이스를 이용해 `Querydsl`에서 `Page`를 반환하도록 하겠습니다.

먼저 `CustomPlayerRepository`에 새로운 메서드를 추가해줍니다. 

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomPlayerRepository {
    List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam);

    Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable); // (1)
}
```

> (1) 기존 처럼 파라미터를 받으면서 페이징 관련 속성도 추가로 받고, `Page` 인터페이스를 반환하는데 그 때 내용은 `PlayerDetails`가 되도록 설계하였습니다.

구현체에서 구현해줘야겠죠?

`PlayerRepositoryImpl` 클래스에 메서드를 구현해줍니다.

```java
@Override
public Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
    QueryResults<PlayerDetails> playerDetails = queryFactory
            .select(Projections.bean(PlayerDetails.class, player.name, player.age, player.inSeason, player.weeklySalary, team.name.as("teamName")))
            .from(player)
            .leftJoin(player.team, team)
            .where(condition(playerQueryParam.getName(), player.name::eq),
                    condition(playerQueryParam.getAge(), player.age::eq),
                    condition(playerQueryParam.getTeamName(), team.name::eq))
            .offset(pageable.getOffset()) // (1)
            .limit(pageable.getPageSize()) // (2)
            .fetchResults(); // (3)
    return new PageImpl<>(playerDetails.getResults(), pageable, playerDetails.getTotal()); // (4)
}

private <T> BooleanExpression condition(T value, Function<T, BooleanExpression> function) {
    return Optional.ofNullable(value).map(function).orElse(null);
}
```

> (1) `offset`을 페이지 시작 지점으로 지정합니다.  
> (2) `limit`를 페이지 사이즈로 지정합니다.  
> (3) `fetchResults`를 수행하면 `totalCount`도 같이 조회합니다.    
> (4) `Page`의 구현체를 생성해 필요한 데이터를 생성자로 넘겨줍니다.

이제 테스트 코드를 추가할 차례입니다.

페이징 기능이 들어가려면 데이터가 넉넉히 있어야겠죠?

`@BeforeEach`에서 토트넘 선수들을 더 추가하였고 기존 테스트에서 검증하는 값을 수정했습니다.

이전까지의 포스팅에서 사용된 검증 값은 이후 제대로 동작하지 않으니 아래 처럼 모두 수정해야 합니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.Team;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PlayerRepositoryTest {
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setup() {
        Team tottenhamHotspur = new Team("Tottenham Hotspur F.C.");
        Team manchesterCity = new Team("Manchester City F.C.");
        entityManager.persist(tottenhamHotspur);
        entityManager.persist(manchesterCity);

        Player harryKane = new Player("Harry Kane", 27, tottenhamHotspur);
        harryKane.contactSalary(200000);
        harryKane.begins();
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        heungminSon.contactSalary(140000);
        heungminSon.begins();
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        kevinDeBruyne.contactSalary(350000);
        kevinDeBruyne.begins();
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);
        raheemSterling.contactSalary(300000);
        raheemSterling.begins();
        Player deleAlli = new Player("Dele Alli", 25, tottenhamHotspur);
        deleAlli.contactSalary(100000);
        deleAlli.begins();
        Player hugoLloris = new Player("Hugo Lloris", 34, tottenhamHotspur);
        hugoLloris.contactSalary(10000);
        hugoLloris.begins();
        Player tobyAlderweireld = new Player("Toby Alderweireld", 32, tottenhamHotspur);
        tobyAlderweireld.contactSalary(80000);
        tobyAlderweireld.begins();
        Player moussaSissoko = new Player("Moussa Sissoko", 31, tottenhamHotspur);
        moussaSissoko.contactSalary(80000);
        moussaSissoko.begins();
        Player erikLamela = new Player("Erik Lamela", 29, tottenhamHotspur);
        erikLamela.contactSalary(80000);
        erikLamela.begins();
        Player lukasMoura = new Player("Lukas Moura", 28, tottenhamHotspur);
        lukasMoura.contactSalary(80000);
        lukasMoura.begins();

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        entityManager.persist(deleAlli);
        entityManager.persist(hugoLloris);
        entityManager.persist(tobyAlderweireld);
        entityManager.persist(moussaSissoko);
        entityManager.persist(erikLamela);
        entityManager.persist(lukasMoura);
    }

    @Test
    void testBasicFunctions() {
        // when
        List<Player> players = playerRepository.findAll();
        // then
        assertEquals(10, players.size());
        // print
        players.forEach(System.out::println);

        // when
        List<Player> playerByName = playerRepository.findByName("Heungmin Son");
        // then
        assertEquals(1, playerByName.size());
        assertEquals("Heungmin Son", playerByName.get(0).getName());
        //print
        playerByName.forEach(System.out::println);
    }

    @Test
    void dynamicQueryTest() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setName("Heungmin Son");
        playerQueryParam.setAge(29);
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        List<PlayerDetails> players = playerRepository.findPlayerTeamBy(playerQueryParam);

        // then
        assertEquals(1, players.size());

        // print
        players.forEach(System.out::println);
    }

    @Test
    void dynamicQueryTest2() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        List<PlayerDetails> players = playerRepository.findPlayerTeamBy(playerQueryParam);

        // then
        assertEquals(8, players.size());

        // print
        players.forEach(System.out::println);
    }

    @Test
    void pagingTest() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        Page<PlayerDetails> players = playerRepository.findPlayerTeamPageBy(playerQueryParam, PageRequest.of(0, 3)); // (1)

        // then
        assertEquals(3, players.getSize()); // (2)
        assertEquals(3, players.getTotalPages()); // (3)
        assertEquals(8, players.getTotalElements()); // (4)

        // print
        players.forEach(System.out::println);
    }
}
```

> (1) 0 페이지 부터 시작하여(offset) 3 개씩 조회(pageSize)하도록 하였습니다.  
> (2) 3개가 반환됩니다.  
> (3) 등록한 토트넘 선수가 8명 이므로 페이지는 총 3페이지 입니다.  
> (4) 등록한 토트넘 선수가 8명 이므로 총 엘리먼트 수는 8개 입니다.

배보다 배꼽이 더 크네요😭 선수 정보 알아와서 추가한 게 아까워서라도 테스트 케이스를 하나 더 만들어야겠습니다. 😜

일단 결과를 확인해보면,

```text
2021-07-25 02:03:33.848 DEBUG 4882 --- [           main] org.hibernate.SQL                        : 
    /* select
        count(player) 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 */ select
            count(player0_.player_id) as col_0_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=?
2021-07-25 02:03:33.871 DEBUG 4882 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=? limit ?
// 파라미터 바인딩 부분 생략
PlayerDetails(name=Harry Kane, age=27, inSeason=true, weeklySalary=200000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Dele Alli, age=25, inSeason=true, weeklySalary=100000, teamName=Tottenham Hotspur F.C.)
```

테스트는 성공하였고 전체 카운트하는 쿼리와 실제 내용을 조회하는 쿼리, 결과 모두 제대로 출력된 것을 확인할 수 있습니다.

이번엔 정렬 정보까지 포함해보겠습니다.

`PlayerRepositoryImpl`의 `findPlayerTeamPageBy` 메서드를 다음과 같이 수정해줍니다.

```java
@Override
public Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
    QueryResults<PlayerDetails> playerDetails = queryFactory
            .select(Projections.bean(PlayerDetails.class, player.name, player.age, player.inSeason, player.weeklySalary, team.name.as("teamName")))
            .from(player)
            .leftJoin(player.team, team)
            .where(condition(playerQueryParam.getName(), player.name::eq),
                    condition(playerQueryParam.getAge(), player.age::eq),
                    condition(playerQueryParam.getTeamName(), team.name::eq))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(orderCondition(pageable)) // (1)
            .fetchResults();
    return new PageImpl<>(playerDetails.getResults(), pageable, playerDetails.getTotal());
}

private <T> BooleanExpression condition(T value, Function<T, BooleanExpression> function) {
    return Optional.ofNullable(value).map(function).orElse(null);
}

private OrderSpecifier[] orderCondition(Pageable pageable) {
    PathBuilder<Player> entityPath = new PathBuilder<>(Player.class, "player");
    return pageable.getSort() // (2)
            .stream() // (3)
            .map(order -> new OrderSpecifier(Order.valueOf(order.getDirection().name()), entityPath.get(order.getProperty()))) // (4)
            .toArray(OrderSpecifier[]::new); // (5)
}
```

> (1) `order`를 동적으로 추가하기위해 메서드를 생성하여 호출하였습니다.  
> (2) `pageale.getSort()`는 절대 `null`을 반환하지 않습니다. 아무 것도 입력하지 않은 경우 `Sort.unsorted()`를 호출해 상수 `UNSORTED`를 설정해주기 때문입니다. 따라서 별도로 `null` 체크를 할 필요가 없습니다.  
> (3) `Sort`는 `Streamable`을 구현하고 있기 때문에 바로 `stream()`을 호출할 수 있습니다.  
> (4) `orderBy`에 전달해야 할 타입이 `OrderSpecifier`이기 때문에 해당 타입으로 매핑해줍니다.  
> (5) `orderBy`에는 `0..N` 개의 `OrderSpecifier`를 전달할 수 있습니다. `Sort`에 `Order`가 존재할 가능성(N개가 될 가능성)이 있기 때문에 배열 타입으로 변환해줍니다.

이제 테스트를 추가해봅시다.

```java
@Test
void pagingWithSortingTest() {
    // given
    PlayerQueryParam playerQueryParam = new PlayerQueryParam();
    playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

    // when
    Page<PlayerDetails> players = playerRepository.findPlayerTeamPageBy(playerQueryParam,
        PageRequest.of(0, 3, Sort.by(Sort.Order.asc("weeklySalary")))); // (1)

    // then
    assertEquals(3, players.getSize());
    assertEquals(3, players.getTotalPages());
    assertEquals(8, players.getTotalElements());

    // print
    players.forEach(System.out::println);
}
```

> (1) 0 페이지 부터 3 개씩 주급 오름차순으로 정렬합니다.

테스트를 실행해보면,

```text
// totalCount 조회 쿼리 로그 생략
2021-07-25 02:41:10.569 DEBUG 5117 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 
    order by
        player.weeklySalary asc */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=? 
        order by
            player0_.weekly_salary asc limit ?
// 파라미터 바인딩 로그 생략
PlayerDetails(name=Hugo Lloris, age=34, inSeason=true, weeklySalary=10000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Toby Alderweireld, age=32, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Lukas Moura, age=28, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
```

주급 오름차순으로 정렬되어 3명만 조회되는 것을 확인할 수 있습니다.

### totalCount 별도 조회하기

위에도 설명하였지만 `fetchResult()`를 사용하면 `totalCount`를 알아서 조회하는데 위에 로그에서 확인할 수 있다시피 기존 쿼리와 동일하지만 `id`에 `count` 함수를 사용해 조회합니다.

상황에 따라선 `count` 쿼리가 단순해 질 수 있기 때문에 `count`를 위한 쿼리는 분리해서 관리하는 것이 좋습니다.

지금 예제에서는 큰 차이가 없지만 분리하는 방법을 설명하기 위해 그냥 진행했으니 참고하고 봐주시면 감사하겠습니다. 🙏

먼저 `CustomPlayerRepository`에 메서드를 추가하고 `PlayerRepositoryImpl` 클래스를 수정합니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomPlayerRepository {
    List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam);

    Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable);

    Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable); // (1)
}
```

> (1) `count`를 별도로 처리하는 메서드를 따로 추가해줍니다.

```java
@Override
public Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
    List<PlayerDetails> playerDetails = selectFromWhere(playerQueryParam)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(orderCondition(pageable))
            .fetch(); // (1)

    long totalCount = queryFactory
            .select(player)
            .from(player)
            .leftJoin(player.team, team)
            .where(condition(playerQueryParam.getName(), player.name::eq),
                    condition(playerQueryParam.getAge(), player.age::eq),
                    condition(playerQueryParam.getTeamName(), team.name::eq))
            .fetchCount(); // (2)

    return new PageImpl<>(playerDetails, pageable, totalCount); // (3)
}

private JPAQuery<PlayerDetails> selectFromWhere(PlayerQueryParam playerQueryParam) { // (4)
    return queryFactory
            .select(Projections.bean(PlayerDetails.class, player.name, player.age, player.inSeason, player.weeklySalary, team.name.as("teamName")))
            .from(player)
            .leftJoin(player.team, team)
            .where(condition(playerQueryParam.getName(), player.name::eq),
                    condition(playerQueryParam.getAge(), player.age::eq),
                    condition(playerQueryParam.getTeamName(), team.name::eq));
}

private <T> BooleanExpression condition(T value, Function<T, BooleanExpression> function) {
    return Optional.ofNullable(value).map(function).orElse(null);
}

private OrderSpecifier[] orderCondition(Pageable pageable) {
    PathBuilder<Player> entityPath = new PathBuilder<>(Player.class, "player");
    return pageable.getSort()
            .stream()
            .map(order -> new OrderSpecifier(Order.valueOf(order.getDirection().name()), entityPath.get(order.getProperty())))
            .toArray(OrderSpecifier[]::new);
}
```

> (1) `fetchResult` 대신 `fetch`를 사용해 `List`를 반환받습니다.  
> (2) `fetchCount`를 이용해 `totalCount`를 구합니다.  
> (3) `Page` 구현체에 알맞은 타입을 전달합니다.  
> (4) 다른 메서드에서도 동일한 쿼리를 사용해서 메서드로 추출하였습니다.

이제 테스트를 작성해보겠습니다.

```java
@Test
void pagingWithAnotherCountTest() {
    // given
    PlayerQueryParam playerQueryParam = new PlayerQueryParam();
    playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

    // when
    Page<PlayerDetails> players = playerRepository.findPlayerTeamCountPageBy(playerQueryParam, PageRequest.of(0, 3, Sort.by(Sort.Order.asc("weeklySalary"))));

    // then
    assertEquals(3, players.getSize());
    assertEquals(3, players.getTotalPages());
    assertEquals(8, players.getTotalElements());

    // print
    players.forEach(System.out::println);
}
```

기존에서 테스트 코드와 거의 동일하지만 `findPlayerTeamCountPageBy`를 대신 호출합니다.

테스트 결과 정상수행 되었고 구현된 순서대로 `select`를 먼저하고 `count`를 나중에 하는 로그를 확인할 수 있습니다.

```text
2021-07-25 03:19:40.891 DEBUG 5441 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 
    order by
        player.weeklySalary asc */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=? 
        order by
            player0_.weekly_salary asc limit ?
// 나머지 생략 
    /* select
        count(player) 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 */ select
            count(player0_.player_id) as col_0_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=?
// 나머지 생략
```

위에서도 언급했듯이 이 예제에서는 극적인 효과는 커녕 `totalCount`를 구하는 쿼리 자체가 동일합니다. 분리 방법에 초점을 두고 예제는 예제로만 보셔야 합니다. 😀

### Count 쿼리 최적화

다음과 같은 경우 `count` 쿼리를 생략할 수 있습니다.

* 시작 페이지이면서 실제 내용이 페이지 사이즈보다 작을 때
* 마지막 페이지 일 때

`스프링 데이터 JPA`에서 `Slice`를 사용했던 것과 유사한 기능이라고 생각하시면 됩니다.

`PageableExecutionUtils`를 사용해서 구현할 수 있습니다.

`PlayerRepositoryImpl`의 `findPlayerTeamCountPageBy` 메서드를 아래 처럼 수정해줍니다.

> 글이 길어져 소스 코드 중복되는 부분은 생략하였습니다.

```java
@Override
public Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
    List<PlayerDetails> playerDetails = selectFromWhere(playerQueryParam)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(orderCondition(pageable))
            .fetch();

    JPAQuery<Player> countQuery = queryFactory // (1)
            .select(player)
            .from(player)
            .leftJoin(player.team, team)
            .where(condition(playerQueryParam.getName(), player.name::eq),
                    condition(playerQueryParam.getAge(), player.age::eq),
                    condition(playerQueryParam.getTeamName(), team.name::eq));

    return PageableExecutionUtils.getPage(playerDetails, pageable, countQuery::fetchCount); // (2)
}
```

> (1) `countQuery`를 분리해줍니다.  
> (2) `PageableExecutionUtils.getPage`를 호출하는데 `content`, `pageable`, `countQuery 실행 구현체`를 순서대로 넘겨줍니다. 위에서 `countQuery`를 분리해 준 이유가 여기서 메서드 레퍼런스를 사용하기 위함이었습니다. 직접 `LongSupplier`를 구현하셔도 됩니다.

테스트를 `countQuery`가 호출되지 않게 만들어보겠습니다.

```java
@Test
void pagingWithAnotherCountOptimizationTest() {
    // given
    PlayerQueryParam playerQueryParam = new PlayerQueryParam();
    playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

    // when
    Page<PlayerDetails> players = playerRepository.findPlayerTeamCountPageBy(playerQueryParam, 
        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("weeklySalary")))); // (1)

    // then
    assertEquals(10, players.getSize());
    assertEquals(1, players.getTotalPages());
    assertEquals(8, players.getTotalElements());

    // print
    players.forEach(System.out::println);
}

> (1) 결과보다 페이지 사이즈가 크고, 마지막 페이지가 되도록 페이지 사이즈를 10으로 설정하였습니다. (귀찮아서 한 번에 두 조건 다 해당하도록..😬)

```

테스트를 실행해보면,

```text
2021-07-25 03:39:32.825 DEBUG 5597 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.name,
        player.age,
        player.inSeason,
        player.weeklySalary,
        team.name as teamName 
    from
        Player player   
    left join
        player.team as team 
    where
        team.name = ?1 
    order by
        player.weeklySalary asc */ select
            player0_.name as col_0_0_,
            player0_.age as col_1_0_,
            player0_.in_season as col_2_0_,
            player0_.weekly_salary as col_3_0_,
            team1_.name as col_4_0_ 
        from
            player player0_ 
        left outer join
            team team1_ 
                on player0_.team_id=team1_.team_id 
        where
            team1_.name=? 
        order by
            player0_.weekly_salary asc limit ?
// 파라미터 바인딩 로그 생략
PlayerDetails(name=Hugo Lloris, age=34, inSeason=true, weeklySalary=10000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Lukas Moura, age=28, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Erik Lamela, age=29, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Toby Alderweireld, age=32, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Moussa Sissoko, age=31, inSeason=true, weeklySalary=80000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Dele Alli, age=25, inSeason=true, weeklySalary=100000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000, teamName=Tottenham Hotspur F.C.)
PlayerDetails(name=Harry Kane, age=27, inSeason=true, weeklySalary=200000, teamName=Tottenham Hotspur F.C.)
```

`count` 쿼리가 호출되지 않은 것을 확인할 수 있습니다.

동작 원리는 생각해보면 단순합니다. 

`offset`과 `limit`를 이용해 조회해 온 뒤 `limit` 보다 결과가 작으면 그 결과가 결국 `totalCount` 이므로 추가로 조회할 필요가 없습니다.

그리고 또 다른 경우인 마지막 페이지를 판단하여 `totalCount`를 생략하는 경우는, 기존 요청 `limit`에서 1을 더해 조회하여 그 결과가 `limit+1` 보다 작다면 마지막 페이지라는 것을 알 수 있고, 마지막 페이지인 경우 `offset * size + 조회된 결과 수`가 `totalCount`가 됩니다.

> 혹시 헷갈리실 분을 위해 예를 들자면, 전체 데이터가 100개, `offset`이 9, `limit`가 10인 경우 조회할 때 `limit`에 1을 더해 91번 부터 101번까지 11개를 조회하고, 그 결과 91번에서 100번까지 10개만 조회되므로 마지막 페이지임을 알 수 있습니다.
> `totalCount`는 9(offset) * 10(limit) + 10(result) = 100 입니다.

---

여기까지 `스프링 데이터 JPA`와 `Querydsl`을 같이 사용하는 방법에 대해서 알아보았습니다.

다음 포스팅에서는 `스프링 데이터 JPA`가 `Querydsl`을 지원하기위해 제공하는 기능에 대해 알아보겠습니다.