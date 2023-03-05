package com.doi3001.web.repository;

import com.doi3001.web.entity.Temi;
import lombok.Synchronized;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Repository
public class TemiRepository {
    private List<Temi> temis = new LinkedList<>();;

    public TemiRepository() {}

    @Synchronized
    public List<Temi> findAll() {
        return temis;
    }

    @Synchronized
    public Temi create(String name, String ip) {
        Temi temi = new Temi(name, ip);
        temis.add(temi);

        return temi;
    }

    public Temi find(String uuid) {
        return find(UUID.fromString(uuid));
    }

    @Synchronized
    public Temi find(UUID uuid) {
        Temi temi = temis.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);

        return temi;
    }

    public Temi update(String uuid, Temi request) {
        return update(UUID.fromString(uuid), request);
    }

    @Synchronized
    public Temi update(UUID uuid, Temi request) {
        Temi temi = temis.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);


        if (temi != null) {
            if (request.getName() != null) {
                temi.setName(request.getName());
            }
            if (request.getIp() != null) {
                temi.setIp(request.getIp());
            }
        }

        return temi;
    }

    public Temi delete(String uuid) {
        return delete(UUID.fromString(uuid));
    }

    @Synchronized
    public Temi delete(UUID uuid) {
        Temi temi = temis.stream().filter(x -> x.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);


        if (temi != null) {
            temis.remove(temi);
        }

        return temi;
    }
}
