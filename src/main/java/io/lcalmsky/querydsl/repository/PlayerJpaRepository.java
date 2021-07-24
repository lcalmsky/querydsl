package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerJpaRepository {

    private final EntityManager entityManager;

    public void save(Player player) {
        entityManager.persist(player);
    }

    public Optional<Player> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Player.class, id));
    }

    public List<Player> findAll() {
        return entityManager.createQuery("select p from Player p", Player.class)
                .getResultList();
    }

    public List<Player> findByName(String name) {
        return entityManager.createQuery("select p from Player p where p.name = :name", Player.class)
                .setParameter("name", name)
                .getResultList();
    }
}
