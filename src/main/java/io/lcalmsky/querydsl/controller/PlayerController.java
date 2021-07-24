package io.lcalmsky.querydsl.controller;

import io.lcalmsky.querydsl.domain.PlayerWithTeamData;
import io.lcalmsky.querydsl.domain.param.PlayerQueryParam;
import io.lcalmsky.querydsl.repository.PlayerQuerydslRepository;
import io.lcalmsky.querydsl.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerQuerydslRepository playerQuerydslRepository;
    private final PlayerRepository playerRepository;

    @GetMapping("/v1/players")
    public List<PlayerWithTeamData> searchPlayersV1(PlayerQueryParam param) {
        return playerQuerydslRepository.findPlayerTeamBy(param);
    }

//    @GetMapping("/v2/players")
//    public List<PlayerWithTeamData> searchPlayersV2(PlayerQueryParam param) {
//        return playerRepository.findPlayerTeamBy(param);
//    }
}
