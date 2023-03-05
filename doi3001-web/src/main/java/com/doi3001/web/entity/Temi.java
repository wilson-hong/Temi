package com.doi3001.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@Getter
@Setter
public class Temi {
    private final UUID uuid;
    private String name;
    private String ip;
    private String state = "idle";
    @JsonIgnore
    private UUID playerUUID;
    @JsonIgnore
    private WebSocketSession webSocketSession;

    public Temi(String name, String ip) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.ip = ip;
    }
}
