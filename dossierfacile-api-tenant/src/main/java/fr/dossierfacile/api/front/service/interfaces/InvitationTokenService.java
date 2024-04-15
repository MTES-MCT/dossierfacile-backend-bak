package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.InvitationToken;

import java.util.List;

public interface InvitationTokenService {
    List<InvitationToken> findExpiredInvitation();

    void delete(InvitationToken invitationToken);
}
