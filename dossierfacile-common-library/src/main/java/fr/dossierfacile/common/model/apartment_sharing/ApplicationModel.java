package fr.dossierfacile.common.model.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationModel {
    private Long id;
    private ApplicationType applicationType;
    private PartnerCallBackType partnerCallBackType;
    private Long onTenantId;
    private String dossierUrl;
    private String dossierPdfUrl;
    private FileStatus dossierPdfDocumentStatus;
    private TenantFileStatus status;
    private List<TenantModel> tenants;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime lastUpdateDate;
    @JsonIgnore
    private UserApi userApi = null;
}
