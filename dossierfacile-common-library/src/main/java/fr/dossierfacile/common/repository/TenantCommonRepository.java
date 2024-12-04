package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.TenantUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantCommonRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByEmail(String email);

    Optional<Tenant> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    @Query("select distinct t FROM Tenant t where concat(LOWER(coalesce(t.firstName, '')),' ',LOWER(coalesce(t.lastName, ''))) like CONCAT('%', :nameUser, '%')")
    Page<Tenant> findTenantByFirstNameOrLastNameOrFullName(@Param("nameUser") String nameUser, Pageable pageable);

    @Query("select t from Tenant t " +
            " where (t.operatorDateTime is null or t.operatorDateTime < :localDateTime)" +
            " and t.status = 'TO_PROCESS' and t.honorDeclaration = true " +
            " order by t.lastUpdateDate")
    Page<Tenant> findTenantsToProcess(@Param("localDateTime") LocalDateTime localDateTime, Pageable pageable);


    // rank condition is set to avoid to treat too fast a returning tenant
    // status and honorDeclaration are redundancy but that okay
    @Query(value = """
            SELECT t.*,ua.*
            FROM ranked_tenant rt
              INNER JOIN tenant t ON rt.tid=t.id and rt.last_update_date=t.last_update_date
              INNER JOIN user_account ua ON t.id=ua.id
            WHERE (t.operator_date_time IS NULL OR t.operator_date_time < :toLocalDateTime)
              AND t.status = 'TO_PROCESS'
              AND t.honor_declaration = true
              AND rank < 200
            ORDER BY
             CASE WHEN rt.operator_id = :operatorId THEN 0 ELSE 1 END,
             rt.rank
            LIMIT 1
            """, nativeQuery = true)
    Tenant findMyNextApplication(@Param("toLocalDateTime") LocalDateTime toLocalDateTime,
                                 @Param("operatorId") Long operatorId);

    @Procedure(procedureName = "refresh_mv")
    void refreshMaterializedView(@Param("viewName") String viewName);

    default void refreshRank() {
        refreshMaterializedView("latest_operator");
        refreshMaterializedView("ranked_tenant");
    }

    @Query("""
            SELECT t
            FROM Tenant t
            WHERE t.status = 'TO_PROCESS'
              AND t.honorDeclaration = true
            ORDER BY t.lastUpdateDate ASC
            """)
    Page<Tenant> findToProcessApplicationsByOldestUpdateDate(Pageable pageable);

    List<Tenant> findAllByApartmentSharingId(Long ap);

    Tenant findOneById(Long id);

    @Query(
            value = "select distinct tl.tenant_id from tenant_log as tl \n" +
                    "left join tenant_userapi tu on tl.tenant_id = tu.tenant_id \n" +
                    "where tu.userapi_id=:id and tl.log_type = 'ACCOUNT_VALIDATED' \n" +
                    "and tl.creation_date >= :since",
            nativeQuery = true
    )
    List<Long> listIdTenantsAccountCompletedPendingToSendCallBack(@Param("id") Long userApiId, @Param("since") LocalDateTime lastUpdateSince);

    @Query(value = """
            SELECT COUNT(DISTINCT t.id)
            FROM tenant t
            LEFT JOIN document d ON d.tenant_id = t.id OR d.guarantor_id = t.id
            WHERE d.watermark_file_id IS NULL
              AND d.document_status IS NOT NULL
              AND ( d.last_modified_date IS NULL OR d.last_modified_date < NOW() - INTERVAL '1' HOUR)
            """, nativeQuery = true)
    long countAllTenantsWithoutPdfDocument();

    @Query(value = """
            SELECT *
            FROM tenant t
            JOIN user_account u ON t.id = u.id
            WHERE t.id IN (
              SELECT t2.id
              FROM tenant t2
              JOIN document d ON d.tenant_id = t2.id
              WHERE d.watermark_file_id IS NULL
                AND d.document_status IS NOT NULL
                AND ( d.last_modified_date IS NULL OR d.last_modified_date < NOW() - INTERVAL '1' HOUR)
              UNION DISTINCT
              SELECT t3.id
              FROM tenant t3
              JOIN guarantor g ON g.tenant_id = t3.id
              JOIN document d ON d.guarantor_id = g.id
              WHERE d.watermark_file_id IS NULL
                AND d.document_status IS NOT NULL
                AND ( d.last_modified_date IS NULL OR d.last_modified_date < NOW() - INTERVAL '1' HOUR)
            )
            ORDER BY t.last_update_date DESC
            """, nativeQuery = true)
    Page<Tenant> findAllTenantsToProcessWithoutPdfDocument(Pageable pageable);

    long countAllByStatus(TenantFileStatus tenantFileStatus);
    //endregion

    //region Used in FO
    Optional<Tenant> findByEmailAndEnabledFalse(String email);

    @Query("from Tenant t where t.email in :emails")
    List<Tenant> findByListEmail(@Param("emails") List<String> emails);

    List<Tenant> findAllByEnabledIsFalseAndCreationDateTimeIsBetween(LocalDateTime initDate, LocalDateTime endDate);

    @Query("from Tenant t " +
            "join TenantLog l on t.id = l.tenantId " +
            "where t.honorDeclaration = false and l.logType = 'ACCOUNT_EDITED' and l.creationDateTime between :initDate and :endDate")
    List<Tenant> findAllByHonorDeclarationIsFalseAndCompletionDateTimeIsBetween(@Param("initDate") LocalDateTime initDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "select distinct t2 from Tenant t2 " +
                    " join fetch t2.apartmentSharing a " +
                    " join fetch a.tenants ts " +
                    " join TenantLog l on t2.id = l.tenantId " +
                    " where " +
                    " t2.lastUpdateDate < :startDate " +
                    " and " +
                    " l.creationDateTime between :startDate and :endDate " +
                    " and " +
                    " l.logType = 'ACCOUNT_DENIED' " +
                    " and " +
                    " ( " +
                    "   t2.id in ( " +
                    "        select t.id from Tenant t" +
                    "        join t.documents d" +
                    "        join t.apartmentSharing a " +
                    "        where d.documentStatus = 'DECLINED'" +
                    "   ) " +
                    "   or " +
                    "   t2.id in (" +
                    "        select t.id from Tenant t" +
                    "        join t.guarantors g" +
                    "        join g.documents d" +
                    "        join t.apartmentSharing a" +
                    "        where d.documentStatus = 'DECLINED'" +
                    "   ) " +
                    " )"
    )
    List<Tenant> findAllDeclinedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT COUNT(t.id)
            FROM tenant t
            LEFT JOIN document d ON d.tenant_id = t.id OR d.guarantor_id = t.id
            WHERE t.apartment_sharing_id = :apartmentSharingId
              AND ( t.status != 'VALIDATED' OR d.watermark_file_id IS NULL )
            """, nativeQuery = true)
    int countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(@Param("apartmentSharingId") long apartmentSharingId);

    @Query("""
            SELECT DISTINCT t
            FROM Tenant t
            JOIN TenantLog l ON t.id = l.tenantId
            LEFT JOIN Fetch t.tenantsUserApi
            WHERE l.creationDateTime BETWEEN :startDate AND :endDate
              AND l.logType = 'ACCOUNT_VALIDATED'
            """
    )
    List<Tenant> findAllTenantsValidatedSinceXDaysAgo(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Tenant> findAllByApartmentSharing(ApartmentSharing apartmentSharing);

    List<Tenant> findByEmailInAndApartmentSharingNot(List<String> coTenantEmail, ApartmentSharing apartmentSharing);

    Tenant findByKeycloakId(String keycloakId);

    @Query(value = """
            SELECT * FROM (
                SELECT tenant_id as id,
                 CAST(null AS bigint) as apartmentSharingId,
                 CAST(null AS timestamp) as lastUpdateDate,
                 CAST(null AS timestamp) as creationDate,
                 creation_date as deletionDate,
                 CAST(null AS timestamp) as revocationDate
                FROM tenant_log
                WHERE  (CAST(CAST(:lastUpdateFrom AS text) AS timestamp) IS NULL
                 OR creation_date > CAST(CAST(:lastUpdateFrom AS text) AS timestamp))
                 AND log_type = 'ACCOUNT_DELETE'
                 AND :partnerId = ANY (user_apis)
                 AND :includeDeleted
                UNION
                (SELECT tenant_id as id,
                 CAST(null AS bigint) as apartmentSharingId,
                 CAST(null AS timestamp) as lastUpdateDate,
                 CAST(null AS timestamp) as creationDate,
                 CAST(null AS timestamp) as deletionDate,
                 creation_date as revocationDate
                FROM tenant_log tl
                WHERE  (CAST(CAST(:lastUpdateFrom AS text) AS timestamp) IS NULL
                 OR creation_date > CAST(CAST(:lastUpdateFrom AS text) AS timestamp))
                 AND log_type = 'PARTNER_ACCESS_REVOKED'
                 AND :partnerId = ANY (user_apis)
                 AND NOT EXISTS (SELECT 1 FROM tenant_userapi tua WHERE tua.tenant_id = tl.tenant_id AND tua.userapi_id = :partnerId)
                 AND :includeRevoked
                ORDER BY revocationDate DESC LIMIT 1)
                UNION
                SELECT t.id as id,
                 t.apartment_sharing_id as apartmentSharingId,
                 t.last_update_date as lastUpdateDate,
                 ua.creation_date as creationDate,
                 CAST(null AS timestamp) as deletionDate,
                 CAST(null AS timestamp) as revocationDate
                FROM tenant t
                 INNER JOIN user_account ua ON ua.id = t.id
                 INNER JOIN tenant_userapi tua ON tua.tenant_id = t.id
                WHERE tua.userapi_id = :partnerId
                 AND (CAST(CAST(:lastUpdateFrom AS text) AS timestamp) IS NULL OR t.last_update_date > CAST(CAST(:lastUpdateFrom AS text) AS timestamp))
                    ) AS tenantupdate
            ORDER BY lastUpdateDate ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<TenantUpdate> findTenantUpdateByLastUpdateAndPartner(@Param("lastUpdateFrom") LocalDateTime from,
                                                              @Param("partnerId") Long id, @Param("limit") Long limit,
                                                              @Param("includeDeleted") boolean includeDeleted,
                                                              @Param("includeRevoked") boolean includeRevoked);

    @Query(value = """
            SELECT t.id as id, t.apartment_sharing_id as apartmentSharingId, t.last_update_date as lastUpdateDate, ua.creation_date as creationDate 
            FROM  user_account ua 
            INNER JOIN tenant t ON t.id = ua.id
            INNER JOIN tenant_userapi tua ON tua.tenant_id = t.id  
            WHERE tua.userapi_id = :partnerId
            AND ( CAST( CAST(:creationDateFrom AS text) AS timestamp) IS NULL OR ua.creation_date > CAST( CAST(:creationDateFrom AS text) AS timestamp))
            ORDER BY ua.creation_date ASC
            LIMIT :limit
            """, nativeQuery = true
    )
    List<TenantUpdate> findTenantUpdateByCreationDateAndPartner(@Param("creationDateFrom") LocalDateTime from, @Param("partnerId") Long id, @Param("limit") Long limit);

    @Query(value = """
            SELECT rank
            FROM ranked_tenant
            WHERE tid = :tenantId
            LIMIT 1
            """, nativeQuery = true)
    Long getTenantRank(@Param("tenantId") Long tenantId);
}
