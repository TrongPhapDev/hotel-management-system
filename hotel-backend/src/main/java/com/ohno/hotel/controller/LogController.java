package com.ohno.hotel.controller;

import com.ohno.hotel.entity.NhatKyHeThong;
import com.ohno.hotel.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/log")
public class LogController {
    private final LogService service;

    public LogController(LogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NhatKyHeThong>> getAll() {
        return ResponseEntity.ok(service.getAllLogs());
    }
}
