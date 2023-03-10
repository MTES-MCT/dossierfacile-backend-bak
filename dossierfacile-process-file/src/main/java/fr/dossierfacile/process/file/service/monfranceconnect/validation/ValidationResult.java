package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.MonFranceConnectValidationResult;
import fr.dossierfacile.common.enums.MonFranceConnectValidationStatus;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.API_ERROR;
import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.WRONG_CATEGORY;

@Getter
@ToString
@AllArgsConstructor
public class ValidationResult {

    private final File file;
    private final DocumentVerifiedContent content;
    private final QrCode qrCode;
    private final MonFranceConnectValidationStatus validationStatus;

    static ValidationResult error(File file, QrCode qrCode) {
        return new ValidationResult(file, null, qrCode, API_ERROR);
    }

    public static ValidationResult wrongCategory(File file, QrCode qrCode) {
        return new ValidationResult(file, null, qrCode, WRONG_CATEGORY);
    }

    public MonFranceConnectValidationResult toEntity() {
        MonFranceConnectValidationResult entity = new MonFranceConnectValidationResult();
        entity.setFile(file);
        entity.setQrCodeContent(qrCode.getContent());
        entity.setApiResponse(Optional.ofNullable(content)
                .map(DocumentVerifiedContent::getElements)
                .orElse(null));
        entity.setValidationStatus(validationStatus);
        return entity;
    }

}
