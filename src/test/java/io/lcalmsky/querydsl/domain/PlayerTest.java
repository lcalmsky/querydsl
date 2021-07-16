package io.lcalmsky.querydsl.domain;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
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
        Player founded = queryFactory.selectFrom(QPlayer.player)
                .where(QPlayer.player.name.like("%Son"),
                        QPlayer.player.age.lt(30),
                        QPlayer.player.team.name.ne("Manchester City F.C."))
                .fetchOne();
        // then
        assertNotNull(founded);
        assertEquals("Heungmin Son", founded.getName());
    }
}