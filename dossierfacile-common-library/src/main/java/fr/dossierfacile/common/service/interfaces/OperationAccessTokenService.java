package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.OperationAccessToken;

import java.util.List;

public interface OperationAccessTokenService {
    List<OperationAccessToken> findExpiredToken();

    void delete(OperationAccessToken operationAccessToken);

    void revoke(OperationAccessToken operationAccessToken);

    OperationAccessToken save(OperationAccessToken token);

    OperationAccessToken findByToken(String token);
}