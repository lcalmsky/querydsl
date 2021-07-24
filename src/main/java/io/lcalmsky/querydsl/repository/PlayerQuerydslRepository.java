package io.lcalmsky.querydsl.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.lcalmsky.querydsl.domain.Player;
import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;

@Repository
@Transactional
public class PlayerQuerydslRepository {

    @PersistenceContext
    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    public PlayerQuerydslRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public void save(Player player) {
        entityManager.persist(player);
    }

    public void saveAll(Collection<Player> players) {
        for (Player player : players) {
            entityManager.persist(player);
        }
    }

    public Optional<Player> findById(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(player)
                .where(player.id.eq(id))
                .fetchOne());
    }

    public List<Player> findAll() {
        return queryFactory
                .selectFrom(player)
                .fetch();
    }

    public List<Player> findByName(String name) {
        return queryFactory
                .selectFrom(player)
                .where(player.name.eq(name))
                .fetch();
    }

    public List<PlayerWithTeamData> findPlayerTeamBy(PlayerQueryParam playerQueryParam) {

        return queryFactory
                .select(Projections.bean(PlayerWithTeamData.class, player.name, player.age, team.name.as("teamName")))
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

    private <T1, T2> BooleanExpression biCondition(T1 first, T2 second, BiFunction<T1, T2, BooleanExpression> function) {
        return Optional.ofNullable(first)
                .filter(f -> second != null)
                .map(f -> function.apply(f, second))
                .orElse(null);
    }
}