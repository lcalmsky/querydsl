package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.Team;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PlayerQuerydslRepositoryTest {
    @Autowired
    PlayerQuerydslRepository playerQuerydslRepository;
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
    void basicFunctionTest() {
        Player player = new Player("Heungmin Son", 29);
        playerQuerydslRepository.save(player);

        Optional<Player> playerById = playerQuerydslRepository.findById(player.getId());
        assertEquals(player, playerById.orElse(null));

        List<Player> players = playerQuerydslRepository.findAll();
        assertEquals(1, players.size());

        List<Player> playerByName = playerQuerydslRepository.findByName("Heungmin Son");
        assertEquals(1, players.size());
        assertEquals("Heungmin Son", playerByName.get(0).getName());
    }

    @Test
    void dynamicQueryTest() {
        // given
        PlayerQueryParam playerQueryParam = new PlayerQueryParam();
        playerQueryParam.setName("Heungmin Son");
        playerQueryParam.setAge(29);
        playerQueryParam.setTeamName("Tottenham Hotspur F.C.");

        // when
        List<PlayerWithTeamData> players = playerQuerydslRepository.findPlayerTeamBy(playerQueryParam);

        // then
        assertEquals(1, players.size());

        // print
        players.forEach(System.out::println);
    }
}