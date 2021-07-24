package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
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
class PlayerJpaRepositoryTest {
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlayerJpaRepository playerJpaRepository;

    @Test
    void testBasicFunctions() {
        Player player = new Player("Heungmin Son", 29);
        playerJpaRepository.save(player);

        Optional<Player> playerById = playerJpaRepository.findById(player.getId());
        assertEquals(player, playerById.orElse(null));

        List<Player> players = playerJpaRepository.findAll();
        assertEquals(1, players.size());

        List<Player> playerByName = playerJpaRepository.findByName("Heungmin Son");
        assertEquals(1, players.size());
        assertEquals("Heungmin Son", playerByName.get(0).getName());
    }
}