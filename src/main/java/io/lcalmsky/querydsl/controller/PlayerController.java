package io.lcalmsky.querydsl.controller;

import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import io.lcalmsky.querydsl.repository.PlayerQuerydslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerQuerydslRepository playerQuerydslRepository;

    @GetMapping("/v1/players")
    public List<PlayerWithTeamData> searchPlayers(PlayerQueryParam param) {
        return playerQuerydslRepository.findPlayerTeamBy(param);
    }
}
