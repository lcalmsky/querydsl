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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static io.lcalmsky.querydsl.domain.QPlayer.player;
import static io.lcalmsky.querydsl.domain.QTeam.team;

public class PlayerRepositorySupportedImpl extends QuerydslRepositorySupport implements CustomPlayerRepository {
    public PlayerRepositorySupportedImpl() {
        super(Player.class);
    }

    @Override
    public List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam) {
        return selectFromWhere(playerQueryParam)
                .fetch();
    }

    @Override
    public Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
        JPQLQuery<PlayerDetails> jpqlQuery = selectFromWhere(playerQueryParam);
        Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, jpqlQuery);
        QueryResults<PlayerDetails> playerDetails = jpqlQuery.fetchResults();
        return new PageImpl<>(playerDetails.getResults(), pageable, playerDetails.getTotal());
    }

    @Override
    public Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable) {
        List<PlayerDetails> playerDetails = selectFromWhere(playerQueryParam)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderCondition(pageable))
                .fetch();

        JPQLQuery<Player> countQuery =
                from(player)
                        .leftJoin(player.team, team)
                        .where(condition(playerQueryParam.getName(), player.name::eq),
                                condition(playerQueryParam.getAge(), player.age::eq),
                                condition(playerQueryParam.getTeamName(), team.name::eq))
                        .select(player);

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
