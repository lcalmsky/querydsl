package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, CustomPlayerRepository{
    List<Player> findByName(String name);
}
