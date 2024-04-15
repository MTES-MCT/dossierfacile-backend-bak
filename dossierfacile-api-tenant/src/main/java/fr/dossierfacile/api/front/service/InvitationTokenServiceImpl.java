package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.InvitationTokenRepository;
import fr.dossierfacile.api.front.service.interfaces.InvitationTokenService;
import fr.dossierfacile.common.entity.InvitationToken;
import fr.dossierfacile.common.entity.OperationAccessToken;
import fr.dossierfacile.common.repository.OperationAccessTokenRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class InvitationTokenServiceImpl implements InvitationTokenService {
    private OperationAccessTokenRepository invitationTokenRepository;
    @Value("${invitation.delay.before.expiration.hours}")
    private Long delayBeforeExpirationInHours;

}
