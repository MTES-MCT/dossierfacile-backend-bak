package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@Entity
@Table(name = "barcode_file_analysis")
@AllArgsConstructor
@NoArgsConstructor
public class BarCodeFileAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    private BarCodeDocumentType documentType;

    private String barCodeContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object verifiedData;

    @Enumerated(EnumType.STRING)
    private FileAuthenticationStatus authenticationStatus;

    private boolean allowedInDocumentCategory = true;

    @Enumerated(EnumType.STRING)
    private BarCodeType barCodeType;

}
