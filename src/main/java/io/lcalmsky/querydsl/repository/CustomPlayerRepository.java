package io.lcalmsky.querydsl.repository;

import io.lcalmsky.querydsl.domain.PlayerDetails;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomPlayerRepository {
    List<PlayerDetails> findPlayerTeamBy(PlayerQueryParam playerQueryParam);

    Page<PlayerDetails> findPlayerTeamPageBy(PlayerQueryParam playerQueryParam, Pageable pageable);

    Page<PlayerDetails> findPlayerTeamCountPageBy(PlayerQueryParam playerQueryParam, Pageable pageable);
}
