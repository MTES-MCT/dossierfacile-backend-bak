package fr.dossierfacile.common.model.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
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
public class DeletedOwnerModel {
    private Long id;
    private String hashedEmail;
    private String hashedLastname;
    private String hashedFirstname;
    private String hashedPreferredName;
    private Boolean franceConnect;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime creationDateTime;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime lastUpdateDate;
}