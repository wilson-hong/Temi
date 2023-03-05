package com.doi3001.web.repository;

import com.doi3001.web.entity.Player;
import lombok.Synchronized;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Repository
public class PlayerRepository {
    private List<Player> players = new LinkedList<>();;

    public PlayerRepository() {}

    @Synchronized
    public List<Player> findAll() {
        return players;
    }

    @Synchronized
    public Player create(String name, String ip) {
        Player player = new Player(name, ip);
        players.add(player);

        return player;
    }

    public Player find(String uuid) {
        return find(UUID.fromString(uuid));
    }

    @Synchronized
    public Player find(UUID uuid) {
        Player player = players.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);

        return player;
    }

    public Player update(String uuid, Player request) {
        return update(UUID.fromString(uuid), request);
    }

    @Synchronized
    public Player update(UUID uuid, Player request) {
        Player player = players.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);


        if (player != null) {
            if (request.getName() != null) {
                player.setName(request.getName());
            }
            if (request.getIp() != null) {
                player.setIp(request.getIp());
            }
        }

        return player;
    }

    public Player delete(String uuid) {
        return delete(UUID.fromString(uuid));
    }

    @Synchronized
    public Player delete(UUID uuid) {
        Player player = players.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);


        if (player != null) {
            players.remove(player);
        }

        return player;
    }
}
