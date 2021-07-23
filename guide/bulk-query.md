![](https://img.shields.io/badge/spring--boot-2.5.2-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue) ![](https://img.shields.io/badge/querydsl-1.0.10-pink)

> 모든 소스 코드는 [여기](https://github.com/lcalmsky/querydsl) 있습니다.

이번엔 `Querydsl`을 이용해 벌크 쿼리를 작성해보겠습니다.

### 벌크 Update

쿼리를 바로 수정하기 전에 `Player`의 시즌/비시즌 상태를 나타내는 `Boolean` 타입 변수 `inSeason`과 주급을 나타내는 `Integer` 타입 변수 `weeklySalary`를 추가해보겠습니다.

```java
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
```

`Entity`를 수정했으면 `compileQuerydsl`을 실행해줘야겠죠?

```shell
> ./gradlew compileQuerydsl
```

> **혹시 `compileQuerydsl` 실행 시 에러가 발생한다면** `clean` 이후 다시 진행해보세요.
> ```shell
> > ./gradlew clean
> ```

실행하고 나면 `QPlayer` 클래스에 시즌 상태를 나타내는 필드가 추가됩니다.

리그가 끝나면 모두 시즌 `off` 상태가 되기 때문에 한 번에 바꿔주는 쿼리를 작성할 예정입니다.

바로 태스트 코드를 작성해보겠습니다.

```java
package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;

import org.springframework.transaction.annotation.Transactional;;
import java.util.Arrays;
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
        harryKane.contactSalary(200000); // (1)
        harryKane.begins(); // (1)
        Player heungminSon = new Player("Heungmin Son", 29, tottenhamHotspur);
        heungminSon.contactSalary(140000); // (1)
        heungminSon.begins(); // (1)
        Player kevinDeBruyne = new Player("Kevin De Bruyne", 30, manchesterCity);
        kevinDeBruyne.contactSalary(350000); // (1)
        kevinDeBruyne.begins(); // (1)
        Player raheemSterling = new Player("Raheem Shaquille Sterling", 26, manchesterCity);
        raheemSterling.contactSalary(300000); // (1)
        raheemSterling.begins(); // (1)

        entityManager.persist(harryKane);
        entityManager.persist(heungminSon);
        entityManager.persist(kevinDeBruyne);
        entityManager.persist(raheemSterling);
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @Test
    void simpleQuerydslWithBulkUpdate() {
        // when
        long affectedRows = queryFactory // (2)
                .update(player) // (3)
                .set(player.inSeason, false)
                .execute(); // (4)
        entityManager.flush();
        entityManager.clear();
        // then
        List<Boolean> actual = queryFactory
                .select(player.inSeason)
                .from(player)
                .fetch();
        assertEquals(Arrays.asList(false, false, false, false), actual);
    }
}
```

> (1) 시즌 상태와 주급 필드가 추가되었기 때문에 `Entity` 생성시 초기화 하는 부분을 추가하였습니다.    
> (2) `update` - `execute`의 반환 값은 영향 받은 `row`의 수이고 타입은 `long` 입니다.  
> (3) `update` 메서드를 사용합니다.  
> (4) `execute`를 호출해 쿼리를 실행시킵니다.

이제 테스트를 실행해볼까요?

```text
2021-07-23 22:24:21.986 DEBUG 13849 --- [           main] org.hibernate.SQL                        : 
    /* update
        Player player 
    set
        player.inSeason = ?1 */ update
            player 
        set
            in_season=?
2021-07-23 22:24:22.012 DEBUG 13849 --- [           main] org.hibernate.SQL                        : 
    /* select
        player.inSeason 
    from
        Player player */ select
            player0_.in_season as col_0_0_ 
        from
            player player0_
```

테스트는 성공했고 마지막에 `update` 하는 부분과 검증을 위해 `select`하는 부분의 쿼리입니다.

매우 익숙한(?) `SQL` 업데이트를 그대로 사용하시면 됩니다.

다음으로 한 단계 더 응용해서 주급이 20만 유로 이하인 선수들의 주급을 10만 유로씩 상승시켜보겠습니다.

```java
@Test
void simpleQuerydslWithBulkUpdate2(){
    // when
    long affectedRows=queryFactory
        .update(player)
        .set(player.weeklySalary,player.weeklySalary.add(100000)) // (1)
        .where(player.weeklySalary.loe(200000))
        .execute();
    entityManager.flush();
    entityManager.clear();
    // then
    List<Player> players=queryFactory.selectFrom(player)
       .fetch();
    assertEquals(2,affectedRows);
    players.forEach(System.out::println);
}
```

> (1) `add`를 사용해 10만 유로만큼 더해줬습니다.

```text
2021-07-23 23:10:13.354 DEBUG 14296 --- [           main] org.hibernate.SQL                        : 
    /* update
        Player player 
    set
        player.weeklySalary = player.weeklySalary + ?1 
    where
        player.weeklySalary <= ?2 */ update
            player 
        set
            weekly_salary=weekly_salary+? 
        where
            weekly_salary<=?
2021-07-23 23:10:13.385 DEBUG 14296 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_
Player(id=3, name=Harry Kane, age=27, inSeason=true, weeklySalary=300000)
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=240000)
Player(id=5, name=Kevin De Bruyne, age=30, inSeason=true, weeklySalary=350000)
Player(id=6, name=Raheem Shaquille Sterling, age=26, inSeason=true, weeklySalary=300000)
```

정상적으로 수행된 것을 확인할 수 있습니다.

주급을 정액으로 일괄 상승하는 것이 아니라 20% 씩 증가시킨다면 어떻게 해야할까요?

```java
@Test
void simpleQuerydslWithBulkUpdate3(){
    // when
    long affectedRows=queryFactory
        .update(player)
        .set(player.weeklySalary,player.weeklySalary.multiply(1.2)) // (1)
        .where(player.weeklySalary.loe(200000))
        .execute();
    entityManager.flush();
    entityManager.clear();
    // then
    List<Player> players=queryFactory.selectFrom(player)
        .fetch();
    assertEquals(2,affectedRows);
    players.forEach(System.out::println);
}
```

> (1) `add` 대신 `multiply`를 사용하였고 20% 상승을 위해 1.2를 곱해줬습니다.

수행 결과는...

```text
2021-07-23 23:11:31.221 DEBUG 14305 --- [           main] org.hibernate.SQL                        : 
    /* update
        Player player 
    set
        player.weeklySalary = player.weeklySalary * ?1 
    where
        player.weeklySalary <= ?2 */ update
            player 
        set
            weekly_salary=weekly_salary*? 
        where
            weekly_salary<=?
2021-07-23 23:11:31.250 DEBUG 14305 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_
Player(id=3, name=Harry Kane, age=27, inSeason=true, weeklySalary=200000)
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000)
Player(id=5, name=Kevin De Bruyne, age=30, inSeason=true, weeklySalary=350000)
Player(id=6, name=Raheem Shaquille Sterling, age=26, inSeason=true, weeklySalary=300000)
```

오잉? 🤔 값이 바뀌지 않았습니다. `flush`와 `clear`를 호출했기 때문에 영속성 컨텍스트에 남아있어서도 아니고 원인이 뭔지 한참 헤맸습니다.

그 이유는 바로 `multiply` 메서드를 추적해보면 알 수 있습니다.

멀리 갈 필요도 없이 `NumberExpression` 클래스만 확인해보면 되는데요,

```java
public<N extends Number & Comparable<N>> NumberExpression<T> multiply(N right){
    return Expressions.numberOperation(getType(),Ops.MULT,mixin,ConstantImpl.create(right));
}
```

`multiply` 메서드는 `Number`의 자식클래스 `Generic` 타입을 파라미터로 받고있습니다.

따라서 앞서 `player.weeklySalary`를 `Integer`로 선언했기 때문에 1.2를 넣더라도 자동으로 `int`로 캐스팅되어 1을 곱하게 됩니다.

해결하기 위해서는 `weeklySalary` 필드를 소숫점 사용 가능한 타입으로 변경하든지, 아니면 같은 `Integer` 타입을 곱해주면 됩니다.

전자의 경우 다시 `compileQuerydsl`을 수행해야하기 때문에 번거로워 화끈하게 주급을 두 배로 인상해주기로 하였습니다.

```java
@Test
void simpleQuerydslWithBulkUpdate3(){
    // when
    long affectedRows=queryFactory
        .update(player)
        .set(player.weeklySalary,player.weeklySalary.multiply(2)) // (1)
        .where(player.weeklySalary.loe(200000))
        .execute();
    entityManager.flush();
    entityManager.clear();
    // then
    List<Player> players=queryFactory.selectFrom(player)
        .fetch();
    assertEquals(2,affectedRows);
    players.forEach(System.out::println);
}
```

> (1) `multiply` 메서드에 `weeklySalary`와 같은 정수 타입인 2를 전달하였습니다.

```text
Player(id=3, name=Harry Kane, age=27, inSeason=true, weeklySalary=400000)
Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=280000)
Player(id=5, name=Kevin De Bruyne, age=30, inSeason=true, weeklySalary=350000)
Player(id=6, name=Raheem Shaquille Sterling, age=26, inSeason=true, weeklySalary=300000)
```

쿼리는 생략하고 출력한 결과만 봤을 때는 정상적으로 두 배 인상된 주급을 확인할 수 있습니다.

> 사실 저는 후자로도 테스트 해봤고 정상적으로 반영된 것을 확인했습니다.  
> 다들 직접 확인해보세요 😁

### 벌크 Delete

다음은 벌크로 데이터를 삭제하는 방법입니다.

마찬가지로 엄청 간단하기 때문에 테스트 코드로 바로 확인해보겠습니다.

```java
@Test
void simpleQuerydslWithBulkDelete(){
    // when
    long affectedRows=queryFactory
        .delete(player) // (1)
        .where(player.weeklySalary.goe(200000))
        .execute();
    // then
    entityManager.flush();
    entityManager.clear();
    assertEquals(affectedRows,3); // (2)
    List<Player> players=queryFactory
        .selectFrom(player)
        .fetch();
    assertEquals(1,players.size()); // (3)
    System.out.println("players = "+players);
}
```

> (1) `update` 대신 `delete`를 사용하고 나머지는 동일합니다.  
> (2) 주급이 20만 유로 이상인 선수는 세 명 이므로 3개의 row가 영향을 받습니다.  
> (3) 세 명의 선수가 DB에서 지워졌기 때문에 한 명의 선수만 검색되어야 합니다.

```text
2021-07-23 23:28:30.455 DEBUG 14468 --- [           main] org.hibernate.SQL                        : 
    /* delete 
    from
        Player player 
    where
        player.weeklySalary >= ?1 */ delete 
        from
            player 
        where
            weekly_salary>=?
2021-07-23 23:28:30.488 DEBUG 14468 --- [           main] org.hibernate.SQL                        : 
    /* select
        player 
    from
        Player player */ select
            player0_.player_id as player_i1_1_,
            player0_.age as age2_1_,
            player0_.in_season as in_seaso3_1_,
            player0_.name as name4_1_,
            player0_.team_id as team_id6_1_,
            player0_.weekly_salary as weekly_s5_1_ 
        from
            player player0_
players = [Player(id=4, name=Heungmin Son, age=29, inSeason=true, weeklySalary=140000)]
```

테스트 결과 슬프게도 손흥민 선수만 남았습니다. 😢

---

`JPA`나 스프링 데이터 `JPA`로 벌크 쿼리를 수행할 때도 많이 복잡한 건 아니었지만 `Querydsl`을 사용하는 것이 훨씬 더 간단한 느낌을 받았습니다.

동적으로 조건이 변하는 상황이라면 더욱 더 적극적으로 사용할 수 있겠네요.

다음 포스팅에서는 `SQL Function`을 사용하는 부분을 다뤄보겠습니다. 🙋

---

> 이번 포스팅을 작성하다가 깨달은 것인데.. 그동안 `org.springframework.transaction.annotation.Transactional` 대신 `javax.transaction.Transactional`를 사용하고 있었더군요 ㅜㅜ 검증보다는 결과를 출력하는 방식으로 테스트 클래스를 작성하다보니 뭐가 잘못됐는지도 한참동안 모르고 있었네요.. 테스트 코드를 제대로 작성하는 것의 소중함을 이렇게 또 깨달았습니다.😥 후딱 기존 포스팅도 다 수정해놓아야겠네요 🏃