package io.lcalmsky.querydsl.domain;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;
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
    void simpleJpaTest() {
        // when
        List<Player> players = entityManager.createQuery("select p from Player p", Player.class)
                .getResultList();

        // then
        for (Player player : players) {
            System.out.print(player);
            System.out.println(player.getTeam());
        }
    }

    @Test
    void simpleQuerydslTest() {
        // when
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Player founded = queryFactory.select(player)
                .from(player)
                .where(player.name.like("%Son"))
                .fetchOne();
        // then
        assertNotNull(founded);
        assertEquals("Heungmin Son", founded.getName());
    }

    @Test
    void simpleQuerydslWithAliasTest() {
        // when
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QPlayer player = new QPlayer("p");
        Player founded = queryFactory.select(player)
                .from(player)
                .where(player.name.like("%Son"))
                .fetchOne();
        // then
        assertNotNull(founded);
        assertEquals("Heungmin Son", founded.getName());
    }

    @Test
    void simpleQuerydslWithWhereClauseTest() {
        // given
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Player founded = queryFactory.selectFrom(player)
                .where(player.name.like("%Son"),
                        player.age.lt(30),
                        player.team.name.ne("Manchester City F.C."))
                .fetchOne();
        // then
        assertNotNull(founded);
        assertEquals("Heungmin Son", founded.getName());
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

    @Test
    void simpleQuerydslWithPaging2() {
        // given
        QueryResults<Player> players = queryFactory.selectFrom(player)
                .orderBy(player.name.asc())
                .offset(1)
                .limit(2)
                .fetchResults();
        // then
        assertEquals(4, players.getTotal());
        assertEquals(2, players.getResults().size());
    }

    @Test
    void simpleQuerydslWithFunction() {
        Tuple players = queryFactory.select(player.count(), player.age.sum(), player.age.avg(), player.age.max(), player.age.min())
                .from(player)
                .fetchOne();
        System.out.println(players);
    }

    @Test
    void simpleQuerydslWithAggregation() {
        List<Tuple> ages = queryFactory.select(team.name, player.age.avg())
                .from(player)
                .join(player.team, team)
                .groupBy(team.name)
                .having(player.age.avg().goe(28))
                .fetch();
        System.out.println(ages);
    }

    @Test
    void simpleQuerydslWithJoin() {
        List<Player> players = queryFactory.select(player)
                .from(player)
                .join(player.team, team)
                .fetch();
        players.forEach(p -> System.out.printf("%s %s%n", p, p.getTeam()));
    }

    @Test
    void simpleQuerydslWithThetaJoin() {
        List<Player> players = queryFactory.select(player)
                .from(player, team)
                .where(player.team.name.eq(team.name))
                .fetch();
        players.forEach(p -> System.out.printf("%s %s%n", p, p.getTeam()));
    }

    @Test
    void simpleQuerydslWithLeftJoinOn() {
        List<Tuple> tuples = queryFactory.select(player, team)
                .from(player)
                .leftJoin(player.team, team)
                .on(team.name.eq("Tottenham Hotspur F.C."))
                .fetch();
        tuples.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithJoinOn() {
        List<Tuple> tuples = queryFactory.select(player, team)
                .from(player)
                .join(player.team, team)
                .on(team.name.eq("Tottenham Hotspur F.C."))
                .fetch();
        tuples.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithJoinWithoutOn() {
        List<Tuple> tuples = queryFactory.select(player, team)
                .from(player)
                .join(player.team, team)
                .where(team.name.eq("Tottenham Hotspur F.C."))
                .fetch();
        tuples.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithOuterJoinOn() {
        List<Tuple> tuples = queryFactory.select(player, team)
                .from(player)
                .leftJoin(team)
                .on(player.team.name.eq(team.name))
                .fetch();
        tuples.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithFetchJoin() {
        Player founded = queryFactory.selectFrom(player)
                .join(player.team, team)
                .fetchJoin()
                .where(player.name.eq("Heungmin Son"))
                .fetchOne();
        assertNotNull(founded);
        assertNotNull(founded.getTeam());
        System.out.println(founded + " " + founded.getTeam());
    }

    @Test
    void simpleQuerydslWithSubQuery1() {
        QPlayer subPlayer = new QPlayer("subPlayer");

        Player founded = queryFactory
                .selectFrom(player)
                .where(player.age.eq(
                        JPAExpressions
                                .select(subPlayer.age.max())
                                .from(subPlayer)))
                .fetchOne();

        assertNotNull(founded);
        assertEquals(founded.getName(), "Kevin De Bruyne");
    }

    @Test
    void simpleQuerydslWithSubQuery2() {
        QPlayer subPlayer = new QPlayer("subPlayer");

        List<Player> players = queryFactory
                .selectFrom(player)
                .where(player.age.goe(
                        JPAExpressions
                                .select(subPlayer.age.avg())
                                .from(subPlayer)))
                .fetch();

        assertNotNull(players);
        assertEquals(2, players.size());
    }

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

    @Test
    void simpleQuerydslWithSubQuery4() {
        QPlayer subPlayer = new QPlayer("subPlayer");

        List<Tuple> players = queryFactory
                .select(player.name, JPAExpressions
                        .select(subPlayer.age.avg())
                        .from(subPlayer))
                .from(player)
                .fetch();

        System.out.println("players = " + players);
    }

    @Test
    void simpleQuerydslWithSimpleCase() {
        List<String> nations = queryFactory
                .select(player.name
                        .when("Heungmin Son").then("대한민국")
                        .when("Harry Kane").then("잉글랜드")
                        .otherwise("기타"))
                .from(player)
                .fetch();
        nations.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithComplexCase() {
        List<String> nations = queryFactory
                .select(new CaseBuilder()
                        .when(player.name.like("%Son")).then("대한민국")
                        .otherwise("기타")
                )
                .from(player)
                .fetch();
        nations.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithConstants() {
        List<Tuple> age = queryFactory
                .select(player.name, Expressions.constant("NAME"))
                .from(player)
                .fetch();
        age.forEach(System.out::println);
    }

    @Test
    void simpleQuerydslWithConcat() {
        List<String> nameWithAge = queryFactory
                .select(player.name.concat(": ").concat(player.age.stringValue()))
                .from(player)
                .orderBy(player.age.desc())
                .fetch();
        nameWithAge.forEach(System.out::println);
    }
}