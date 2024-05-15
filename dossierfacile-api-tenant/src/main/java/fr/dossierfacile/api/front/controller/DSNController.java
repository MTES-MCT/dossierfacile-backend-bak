package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.service.DsnService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/dsn")
@RequiredArgsConstructor
@Slf4j
public class DSNController {
    private final DsnService dsnService;

    @PostMapping(value = "/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addRevenu() {
        log.info("DSN ADD REVENU");
        dsnService.addRevenus();
        return ok().build();
    }

    @PostMapping(value = "/documentProfessional", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> documentProfessional() {
        log.info("DSN ADD REVENU");
        dsnService.addResource();
        return ok().build();
    }
}
