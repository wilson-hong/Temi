package com.doi3001.web.controller;

import com.doi3001.web.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/room")
@RequiredArgsConstructor
public class RoomController {
    private final GameService roomService;

}
