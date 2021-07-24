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
import org.springframework.data.domain.Sort;
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
        Page<PlayerDetails> players = playerRepository.findPlayerTeamPageBy(playerQueryParam, PageRequest.of(0, 3));

        // then
        assertEquals(3, players.getSize());
        assertEquals(3, players.getTotalPages());
        assertEquals(8, players.getTotalElements());

        // print
        players.forEach(System.out::println);
    }

    @Test
    void pagingWithSortingTest() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        Page<PlayerDetails> players = playerRepository.findPlayerTeamPageBy(playerQueryParam, PageRequest.of(0, 3, Sort.by(Sort.Order.asc("weeklySalary"))));

        // then
        assertEquals(3, players.getSize());
        assertEquals(3, players.getTotalPages());
        assertEquals(8, players.getTotalElements());

        // print
        players.forEach(System.out::println);
    }

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

    @Test
    void pagingWithAnotherCountOptimizationTest() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        Page<PlayerDetails> players = playerRepository.findPlayerTeamCountPageBy(playerQueryParam, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("weeklySalary"))));

        // then
        assertEquals(10, players.getSize());
        assertEquals(1, players.getTotalPages());
        assertEquals(8, players.getTotalElements());

        // print
        players.forEach(System.out::println);
    }

}