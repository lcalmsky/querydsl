![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

`스프링 데이터`에서 제공하는 `Querydsl` 기능을 소개합니다.

### Repository 인터페이스 지원: QuerydslPredicateExecutor

> [공식 문서](https://docs.spring.io/spring-data/commons/docs/current/reference/html/#core.extensions.querydsl) 참조 

`스프링 데이터`는 `QuerydslPredicateExecutor`라는 인터페이스를 제공합니다.

```java
public interface QuerydslPredicateExecutor<T> {

    Optional<T> findById(Predicate predicate); // (1)

    Iterable<T> findAll(Predicate predicate); // (2)

    long count(Predicate predicate); // (3)

    boolean exists(Predicate predicate); // (4)

    // … more functionality omitted.
}
```

> (1) Predicate에 매칭되는 하나의 Entity를 반환합니다.  
> (2) Predicate에 매칭되는 모든 Entity를 반환합니다.  
> (3) Predicate에 매칭되는 Entity의 수를 반환합니다.  
> (4) Predicate에 매칭되는 결과가 있는지 여부를 반환합니다.

이 외에도 정렬 정보를 전달한다든지, Page를 반환하게 하는 인터페이스도 존재합니다.

사용 방법은 간단합니다.

기존에 `JpaRepository`를 구현하는 `Repository`에서 `QuerydslPredicateExecutor`도 같이 구현해주면 됩니다.

```java
package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>,
        CustomPlayerRepository, QuerydslPredicateExecutor<Player> { // (1)
    List<Player> findByName(String name);
}

```

> (1) `PlayerRepository`가 `QuerydslPredicateExecutor`를 상속하게 합니다.

테스트를 작성해보겠습니다.

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

import static io.lcalmsky.querydsl.domain.QPlayer.player;

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
    void querydslPredicateExecutorTest() {
        // given
        Predicate ageLessThan = player.age.lt(30); // (1)
        // when
        Iterable<Player> playersAgeLessThan30 = playerRepository.findAll(ageLessThan); // (2)
        List<Player> playersAgeLessThan30List = new ArrayList<>(); 
        playersAgeLessThan30.forEach(playersAgeLessThan30List::add); // (3)
        // then
        assertEquals(6, playersAgeLessThan30List.size());
        // print
        playersAgeLessThan30List.forEach(System.out::println);
    }
}
```

> (1) `Q Type`을 이용해 `Predicate`를 생성합니다.  
> (2) `findAll`과 같은 기본 메서드에 `Q Type`에서 제공하는 메서드 반환 타입인 `Predicate`를 전달할 수 있습니다.  
> (3) `Iterable`을 반환하기 때문에 `Collection` 등으로 다시 변환해서 사용해야 합니다.

```text
2021-07-26 21:51:36.404 DEBUG 17286 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player 
    where
        player.age < ?1 */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_ 
        where
            player0_.age<?
Player(id=3, name=Harry Kane, age=27, inSeason=true, weeklySalary=200000)
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000)
Player(id=6, name=Raheem Shaquille Sterling, age=26, inSeason=true, weeklySalary=300000)
Player(id=7, name=Dele Alli, age=25, inSeason=true, weeklySalary=100000)
Player(id=11, name=Erik Lamela, age=29, inSeason=true, weeklySalary=80000)
Player(id=12, name=Lukas Moura, age=28, inSeason=true, weeklySalary=80000)         
```

원하는 쿼리가 생성되고 결과 또한 정확히 생성된 것을 확인할 수 있습니다.

`and`나 `or` 조건을 이용하기 위해선 `Predicate`의 구현체인 `BooleanExpression`을 사용해야 합니다.

앞에서 많이 다뤘던 부분이기 때문에 간단히 소스 코드만 소개하겠습니다.

```java
@Test
void querydslPredicateExecutorTest2() {
    // given
    BooleanExpression ageLessThan = player.age.lt(30);
    BooleanExpression weeklySalaryLessThan = player.weeklySalary.lt(150000);
    // when
    Iterable<Player> result = playerRepository.findAll(ageLessThan.and(weeklySalaryLessThan)); // (1)
    List<Player> players = new ArrayList<>();
    result.forEach(players::add);
    // then
    assertEquals(4, players.size());
    // print
    players.forEach(System.out::println);
}
```

> (1) `BooleanExpression`으로 선언하였기 떄문에 `and` 등을 사용할 수 있습니다.

이렇게 `QuerydslPredicateExecutor` 인터페이스를 상속하면 `Querydsl`을 사용하기위해 `EntityManager`를 주입하여 `JPAQueryFactory`를 생성하고 기본 쿼리도 직접 작성해야하는 수고를 덜어줄 수 있습니다.

반면 단점도 존재하는데요, 묵시적 `join`(`from` 절에 `join`을 따로 명시하지 않고 `select` 절에서 의존성을 가지는 다른 `Entity`를 조회하려고 하는 경우 `JPA`에서 알아서 `PK`, `FK`를 가지고 `inner join`을 해주는 기능)은 가능하지만 `left join`이 불가능합니다.

이러한 단점 때문에 실무에서는 잘 사용되지 않는 비운의 인터페이스 입니다 🥲

### Querydsl Web 지원

> [공식 문서](https://docs.spring.io/spring-data/commons/docs/current/reference/html/#core.web) 참조

스프링 데이터 모듈은 `Repository` 프로그래밍 모델을 지원하는 다양한 웹 기능을 지원합니다.

이 기능을 사용하기 위해선 `@EnableSpringDataWebSupport` 애너테이션을 추가해줘야 하고 추가했을 때 몇 가지 `Component`를 자동으로 등록해주는데 이 `Component`들이 각 기능을 지원해주는 것들입니다.

이 중 [`Querydsl`을 이용한 `Web` 지원 기능](https://docs.spring.io/spring-data/commons/docs/current/reference/html/#core.web.type-safe)을 살펴보겠습니다.

`API` 설계시 `Predicate`를 이용해 쿼리 파라미터 바인딩을 할 수 있습니다.

간단히 소개만 할 것이기 때문에 따로 소스 코드를 작성하진 않았고 공식 문서의 코드를 가져왔습니다.

```java
@Controller
class UserController {

  @Autowired UserRepository repository;

  @RequestMapping(value = "/", method = RequestMethod.GET)
  String index(Model model, @QuerydslPredicate(root = User.class) Predicate predicate, // (1)    
          Pageable pageable, @RequestParam MultiValueMap<String, String> parameters) {

    model.addAttribute("users", repository.findAll(predicate, pageable));

    return "index";
  }
}
```

> (1) 쿼리 파라미터를 `Predicate`에 매핑해주고 `User Entity`에 사용할 수 있습니다.

여기서 치명적인 단점이 있는데 모든 쿼리파라미터는 `Object Entity`에 해당하는 경우 `eq()`, `Collection Entity`에 해당하는 경우 `contains()`, `Collection Property`에 해당하는 경우 `in()`에 매핑됩니다.

`or`나 `like` 등 다른 기능은 사용할 수 없습니다.

이럴 땐 QuerydslBinderCustomizer를 상속하여 추가로 매핑할 수 있습니다.

```java
interface UserRepository extends CrudRepository<User, String>,
                                 QuerydslPredicateExecutor<User>, // (1)   
                                 QuerydslBinderCustomizer<QUser> { // (2)         

  @Override
  default void customize(QuerydslBindings bindings, QUser user) {

    bindings.bind(user.username).first((path, value) -> path.contains(value)); // (3) 
    bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));  // (4)
    bindings.excluding(user.password); // (5)
  }
}
```

> (1) 앞에서 살펴보았던 `QuerydslPredicateExecutor`로 `Predicate`를 사용할 수 있게 해주는 인터페이스 입니다.  
> (2) `QuerydslBinderCustomizer`를 상속하여 `customize`메서드를 `override`하여 바인딩 방법을 변경해 줄 수 있습니다.  
> (3) `username`에 대해서는 `contains`를 바인딩합니다.  
> (4) `String` 타입에 대해서는 대소문자를 구분하지 않고 `contains`를 바인딩합니다.  
> (5) `User Entity`의 `password` 필드는 바인딩에서 제외합니다.

이렇게 가볍게만 살펴봤는데도 사용하기 위한 조건이 굉장히 까다롭고 복잡합니다.

직관적으로 사용하기 쉽게 설계된 것이 아니기 때문에 굳이 이렇게까지해서 파라미터 바인딩을 사용할 필요가 있을지 고민이 많이 됩니다.

결정적으로 컨트롤러에서 `Querydsl` 관련 기능을 사용하고 그 기능이 그대로 `Repository`에 전달되기 때문에 `Entity`의 구조가 외부에 노출되기 쉽습니다.

> **Warning**: 스프링 데이터 JPA 포스팅 때부터 강조해왔던 점인데 `Entity`가 요청이나 응답에 포함되게해서는 절대 안 됩니다.

### Repository 지원: QuerydslRepositorySupport

`QuerydslRepositorySupport`는 `abstract` 클래스로 `Querydsl`을 사용하는 `Repository` 구현체에서 상속해서 사용합니다.

지원하는 기능을 소개하기 위해 새로운 클래스를 만들어보겠습니다.

```java
package io.lcalmsky.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;

public class PlayerRepositorySupportedImpl extends QuerydslRepositorySupport implements CustomPlayerRepository { // (1)
    public PlayerRepositorySupportedImpl() {
        super(Player.class); // (2)
    }

    @Override
    public List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam) {
        return selectFromWhere(playerQueryParam)
                .fetch();
    }

    @Override
    public Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
        QueryResults<PlayerDetails> playerDetails = selectFromWhere(playerQueryParam)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderCondition(pageable))
                .fetchResults();

        return new PageImpl<>(playerDetails.getResults(), pageable, playerDetails.getTotal());
    }

    @Override
    public Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
        List<PlayerDetails> playerDetails = selectFromWhere(playerQueryParam)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderCondition(pageable))
                .fetch();

        JPQLQuery<Player> countQuery = from(player) // (3)
                        .leftJoin(player.team, team)
                        .where(condition(playerQueryParam.getName(), player.name::eq),
                                condition(playerQueryParam.getAge(), player.age::eq),
                                condition(playerQueryParam.getTeamName(), team.name::eq))
                        .select(player); // (4)

        return PageableExecutionUtils.getPage(playerDetails, pageable, countQuery::fetchCount);
    }

    private JPQLQuery<PlayerDetails> selectFromWhere(PlayerQueryParam playerQueryParam) {
        return from(player)
                .leftJoin(player.team, team)
                .where(condition(playerQueryParam.getName(), player.name::eq),
                        condition(playerQueryParam.getAge(), player.age::eq),
                        condition(playerQueryParam.getTeamName(), team.name::eq))
                .select(Projections.bean(PlayerDetails.class, player.name, player.age, player.inSeason, player.weeklySalary, team.name.as("teamName")));
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
}

```

기존 기능과 동일한 기능을 하도록 작성했습니다.

뭐가 달라졌는지 보이시나요?

> (1) `QuerydslRepositorySupport`를 상속합니다.  
> (2) 생성자에서 `Entity` 클래스만 전달하면 `EntityManager`나 `JPAQueryFactory`를 따로 사용할 필요기 없습니다.  
> (3) `select`가 아닌 `from` 부터 시작합니다.  
> (4) `select`를 가장 마지막에 호출하고 `JPQLQuery`를 반환합니다.

부모 클래스인 `QuerydslRepositorySupport`에서 일부 작업들은 간단히 해주고 부모 메서드 호출을 통해 자식 클래스의 내용이 조금 단순해지는 면도 있지만, `Querydsl` 3 버전 때 만들어진 기능이라 해당 버전에 맞게 구현되어있다보니 `from`을 먼저 호출한다든지 `select` 이후 반환 타입이 `JPQLQuery`라든지 지금 껏 공부해온 내용과는 다소 상이한 면이 있습니다.

`getEntityManager()`를 통해 부모 클래스에 초기화되어있는 `EntityManager`를 가져올 수 있고 `getQuerydsl()`로 특별한 기능을 가진 헬퍼 클래스인 `Querydsl`의 객체 또한 사용 가능합니다.

`Querydsl` 객체를 사용하면 페이징과 같은 기능을 아래처럼 간단하게 사용할 수 있습니다.

```java
@Override
public Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
    JPQLQuery<PlayerDetails> jpqlQuery = selectFromWhere(playerQueryParam); // (1)
    Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, jpqlQuery); // (2)
    QueryResults<PlayerDetails> playerDetails = jpqlQuery.fetchResults();
    return new PageImpl<>(playerDetails.getResults(), pageable, playerDetails.getTotal());
}
```

> (1) 기존 소스 코드에서 페이징 관련 부분을 모두 제외한 뒤 `JPQLQuery` 타입을 반환하였습니다.  
> (2) `Querydsl`의 `applyPagination`을 호출하여 `Pageable` 인터페이스와 위에서 작성한 쿼리를 전달하였습니다. 이렇게하면 알아서 `offset`, `limit`가 전달됩니다.  

여기서 치명적인 단점이 또 하나 있는데요, (2)에도 써있지만 `offset`, `limit`는 정상적으로 매핑시켜주나 `order by` 조건이 동적으로 전달될 경우 정확하게 동작하지 않습니다.

여기까지 알아봤으면 사실상 더 알아볼 필요는 없겠죠?

슬프지만 `Querydsl` 4버전을 지원하는 기능이 추가될 때까지 기다려야 할 것 같습니다.

아니면 `order by` 등을 쿼리 파라미터로 전달할 필요가 없을 때 사용할 순 있지만 두 가지 버전이 혼재하는 상황에서 뭔가를 조작하다가 잘못될 위험을 감수할 필요가 있을지 잘 모르겠습니다. 😭

---

이렇게 스프링 데이터에서 지원하는 `Querydsl` 기능에 대해 알아보았습니다.

---

여기까지 `Querydsl`에 대한 포스팅을 모두 마쳤습니다. 👏👏👏

나중에 추가된 기능이 있거나 보완해야 할 내용이 있을 때까지 복습을 철저히해서 손가락에 배도록 할 생각입니다.