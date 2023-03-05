package com.doi3001.web.websocket;

import com.doi3001.web.entity.Player;
import com.doi3001.web.entity.Temi;
import com.doi3001.web.repository.PlayerRepository;
import com.doi3001.web.repository.TemiRepository;
import com.doi3001.web.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private ObjectMapper objectMapper;
    private final TemiRepository temiRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public WebSocketHandler(TemiRepository temiRepository, PlayerRepository playerRepository) {
        this.temiRepository = temiRepository;
        this.playerRepository = playerRepository;
        objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        System.out.println(String.format("connect: %s", session.getRemoteAddress()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        System.out.println(String.format("disconnect: %s / %s", session.getRemoteAddress(), status.toString()));

        Map<String, Object> attr = session.getAttributes();
        String type = (String)attr.get("type");
        if (type == null) {
             return;
        }

        if (type.equals("temi")) {
            Temi temi = (Temi)attr.get("temi");
            temiRepository.delete(temi.getUuid());

            List<Temi> temis = temiRepository.findAll();
            String data = objectMapper.writeValueAsString(temis);
            String text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_LIST", data));
            List<Player> players = playerRepository.findAll();
            for (Player player: players) {
                WebSocketSession playerSession =  player.getWebSocketSession();
                if (playerSession != null) {
                    playerSession.sendMessage(new TextMessage(text));
                }
            }
        } else if (type.equals("player")) {
            Player player = (Player)attr.get("player");
            playerRepository.delete(player.getUuid());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        WebSocketMessage msg = objectMapper.readValue(payload, WebSocketMessage.class);
        Map<String, Object> attr = session.getAttributes();

        System.out.println(String.format("payload: %s", payload));
        System.out.println(String.format("obj: %s, %s", msg.cmd, msg.data));

        switch (msg.cmd) {
            case "TEMI_INIT": {
                if (attr.get("type") != null) {
                    return;
                }

                String name = msg.data;
                String ip = session.getRemoteAddress().getAddress().toString().substring(1);
                Temi temi = temiRepository.create(name, ip);
                temi.setWebSocketSession(session);

                attr.put("type", "temi");
                attr.put("temi", temi);

                String data = objectMapper.writeValueAsString(temi);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_INIT_SUCCESS", data));
                session.sendMessage(new TextMessage(text));

                List<Temi> temis = temiRepository.findAll();
                data = objectMapper.writeValueAsString(temis);
                text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_LIST", data));
                List<Player> players = playerRepository.findAll();
                for (Player player: players) {
                    WebSocketSession playerSession =  player.getWebSocketSession();
                    if (playerSession != null) {
                        playerSession.sendMessage(new TextMessage(text));
                    }
                }
            } break;

            case "PLAYER_INIT": {
                if (attr.get("type") != null) {
                    return;
                }

                if (4 <= playerRepository.findAll().size()) {
                    String text = objectMapper.writeValueAsString(new WebSocketMessage("PLAYER_INIT_FAILED", ""));
                    session.sendMessage(new TextMessage(text));
                    return;
                }

                String name = msg.data;
                String ip = session.getRemoteAddress().getAddress().toString();
                Player player = playerRepository.create(name, ip);
                player.setWebSocketSession(session);

                attr.put("type", "player");
                attr.put("player", player);

                String data = objectMapper.writeValueAsString(player);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("PLAYER_INIT_SUCCESS", data));
                session.sendMessage(new TextMessage(text));
            } break;

            case "GET_TEMI_LIST": {
                List<Temi> temis = temiRepository.findAll();

                String data = objectMapper.writeValueAsString(temis);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_LIST", data));
                session.sendMessage(new TextMessage(text));
            } break;

            case "GET_PLAYER_LIST": {
                List<Player> players = playerRepository.findAll();

                String data = objectMapper.writeValueAsString(players);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("PLAYER_LIST", data));
                session.sendMessage(new TextMessage(text));
            } break;

            case "PLAYER_CONNECTED": {
                String playerUUID = msg.data;

                Temi temi = (Temi) attr.get("temi");
                temi.setState("active");
                temi.setPlayerUUID(UUID.fromString(playerUUID));

                String data = objectMapper.writeValueAsString(temi);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_STATE_CHANGED", data));
                List<Player> players = playerRepository.findAll();
                for (Player player: players) {
                    WebSocketSession playerSession =  player.getWebSocketSession();
                    if (playerSession != null) {
                        playerSession.sendMessage(new TextMessage(text));
                    }
                }
            } break;

            case "PLAYER_DISCONNECTED": {
                Temi temi = (Temi) attr.get("temi");
                temi.setState("idle");
                temi.setPlayerUUID(null);

                String data = objectMapper.writeValueAsString(temi);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("TEMI_STATE_CHANGED", data));
                List<Player> players = playerRepository.findAll();
                for (Player p: players) {
                    WebSocketSession playerSession =  p.getWebSocketSession();
                    if (playerSession != null) {
                        playerSession.sendMessage(new TextMessage(text));
                    }
                }

            } break;

            case "PLAYER_SELECT_TEAM": {
                String team = msg.data;
                Player player = (Player) attr.get("player");
                player.setTeam(team);

                String data = objectMapper.writeValueAsString(player);
                String text = objectMapper.writeValueAsString(new WebSocketMessage("PLAYER_TEAM_CHANGED", data));
                List<Player> players = playerRepository.findAll();
                for (Player p: players) {
                    WebSocketSession playerSession =  p.getWebSocketSession();
                    if (playerSession != null) {
                        playerSession.sendMessage(new TextMessage(text));
                    }
                }

            } break;

            case "GAME_START": {
                List<Player> players = playerRepository.findAll();
                List<Temi> temis = temiRepository.findAll();
                int reds = 0;

                for (Player player: players) {
                    if (player.getTeam().equals("none")) {
                        String text = objectMapper.writeValueAsString(new WebSocketMessage("PLAYER_NOT_READY", ""));
                        session.sendMessage(new TextMessage(text));
                        return;
                    } else if (player.getTeam().equals("red")) {
                        reds++;
                    }
                }

//                if (reds == 0 || reds == players.size()) {
//                    String text = objectMapper.writeValueAsString(new WebSocketMessage("ONE_TEAM", ""));
//                    session.sendMessage(new TextMessage(text));
//                    return;
//                }

                for (Player player: players) {
                    player.setAlive(true);
                }

                String text = objectMapper.writeValueAsString(new WebSocketMessage("GAME_START", ""));
                for (Player player: players) {
                    WebSocketSession playerSession =  player.getWebSocketSession();
                    if (playerSession != null) {
                        playerSession.sendMessage(new TextMessage(text));
                    }
                }
                for (Temi temi: temis) {
                    WebSocketSession temiSession =  temi.getWebSocketSession();
                    if (temiSession != null) {
                        temiSession.sendMessage(new TextMessage(text));
                    }
                }
                session.sendMessage(new TextMessage(text));
            } break;

            case "TEMI_HIT": {
                Temi temi = (Temi) attr.get("temi");
                UUID playerUUID = temi.getPlayerUUID();
                Player player = playerRepository.find(playerUUID);
                if (player != null) {
                    player.setAlive(false);
                }

                String win = checkGame();
                if (!win.isEmpty()) {
                    List<Player> players = playerRepository.findAll();
                    List<Temi> temis = temiRepository.findAll();

//                    for (Player p: players) {
//                        p.setTeam("none");
//                    }

                    String text, data;
                    for (Player p: players) {
                        WebSocketSession playerSession =  p.getWebSocketSession();
                        if (playerSession != null) {
                            data = p.getTeam().equals(win) ? "win" : "lose";
                            p.setTeam("none");
                            text = objectMapper.writeValueAsString(new WebSocketMessage("GAME_END", data));
                            playerSession.sendMessage(new TextMessage(text));
                        }
                    }

                    text = objectMapper.writeValueAsString(new WebSocketMessage("GAME_END", ""));
                    for (Temi t: temis) {
                        WebSocketSession temiSession =  t.getWebSocketSession();
                        if (temiSession != null) {
                            temiSession.sendMessage(new TextMessage(text));
                        }
                    }
                    session.sendMessage(new TextMessage(text));
                }

            } break;

            default:
                break;
        }
    }

    private String checkGame() {
        List<Player> players = playerRepository.findAll();
        int red = 0, blue = 0;

        for (Player player: players) {
            if (player.getTeam().equals("red") && player.isAlive()) {
                red++;
            } else if (player.getTeam().equals("blue") && player.isAlive()) {
                blue++;
            }
        }

        if (red == 0) {
            return "blue";
        } else if (blue == 0) {
            return "red";
        }

        return "";
    }
}