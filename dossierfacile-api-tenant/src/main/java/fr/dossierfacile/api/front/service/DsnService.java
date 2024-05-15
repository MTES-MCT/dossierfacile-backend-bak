package fr.dossierfacile.api.front.service;


import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.register.tenant.DocumentFinancial;
import fr.dossierfacile.api.front.register.tenant.DocumentProfessional;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.DSNContract;
import fr.dossierfacile.common.entity.DSNPayslip;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class DsnService {
    private final String SELECT_2 = """
            WITH A AS (
            SELECT DISTINCT
            ass.id AS ass_id,
            ass.prenoms_certif_sngi,
            ass.nom_famille_certif_sngi,
            ass.date_naissance_certif_sngi,
            ass.commune_naissance_sngi,
            ass.code_dpt_naissance_sngi,
            ass.code_pays_naissance_sngi,
            emp_ass.id_employeur,
            cont.id as contrat_id,
            cont.date_debut_contrat,
            cont.nature_contrat,
            emp.et_enseigne,
            emp.en_raison_sociale,
            CASE
            WHEN cont.nature_contrat = '01' THEN 'Contrat de travail à durée indéterminée de droit privé'
            WHEN cont.nature_contrat = '02' THEN 'Contrat de travail à durée déterminée de droit privé'
            WHEN cont.nature_contrat = '03' THEN 'Contrat de mission (contrat de travail temporaire)'
            WHEN cont.nature_contrat = '07' THEN 'Contrat à durée indéterminée intermittent'
            WHEN cont.nature_contrat = '08' THEN 'Contrat à durée indéterminée intérimaire'
            WHEN cont.nature_contrat = '09' THEN 'Contrat de travail à durée indéterminée de droit public'
            WHEN cont.nature_contrat = '10' THEN 'Contrat de travail à durée déterminée de droit public'
            WHEN cont.nature_contrat = '20' THEN '[FP] Détachement d un agent d une Fonction Publique donnant lieu à pension (ECP)'
            WHEN cont.nature_contrat = '21' THEN '[FP] Détachement d un agent d une Fonction Publique ne donnant pas lieu à pension (ENCP)'
            WHEN cont.nature_contrat = '29' THEN 'Convention de stage (hors formation professionnelle)'
            WHEN cont.nature_contrat = '32' THEN 'Contrat d appui au projet d entreprise'
            WHEN cont.nature_contrat = '50' THEN 'Nomination dans la fonction publique (par arrêté, par décision,…)'
            WHEN cont.nature_contrat = '60' THEN 'Contrat d engagement éducatif'
            WHEN cont.nature_contrat = '70' THEN 'Contrat de soutien et d aide par le travail'
            WHEN cont.nature_contrat = '80' THEN 'Mandat social'
            WHEN cont.nature_contrat = '81' THEN 'Mandat d élu'
            WHEN cont.nature_contrat = '82' THEN 'Contrat de travail à durée indéterminée de Chantier ou d opération'
            WHEN cont.nature_contrat = '89' THEN 'Volontariat de service civique'
            WHEN cont.nature_contrat = '90' THEN 'Autre nature de contrat, convention, mandat'
            WHEN cont.nature_contrat = '91' THEN 'Contrat d engagement maritime à durée indéterminée'
            WHEN cont.nature_contrat = '92' THEN 'Contrat d engagement maritime à durée déterminée'
            WHEN cont.nature_contrat = '93' THEN 'Ligne de service'
            END AS nature_contrat_libelle,
            --cont.date_fin_previsionnelle,
            --cont.date_fin_contrat,
            --cont.dernier_jour_travaille,
            --vers.id as vers_id,
            vers.date_versement,
            vers.mois_declaration,
            vers.remuneration_nette_fiscale,
            vers.montant_net_verse,
            vers.montant_pas
            FROM ddadtassure ass
            INNER JOIN ddadtemployeur_assure emp_ass on ass.id = emp_ass.id_assure
            INNER JOIN ddadtemployeur emp on emp.id = emp_ass.id_employeur
            INNER JOIN ddadtcontrat cont on cont.id_employeur_assure = emp_ass.id
            INNER JOIN ddadtversement vers on vers.id_employeur_assure = emp_ass.id
            WHERE (cont.date_fin_previsionnelle IS NULL OR cont.date_fin_previsionnelle >= CURRENT_DATE - INTERVAL '11 MONTH') -- à modifier avec les vraies valeurs
            AND (cont.date_fin_contrat IS NULL OR cont.date_fin_contrat >= CURRENT_DATE - INTERVAL '11 MONTH') -- à modifier avec les vraies valeurs
            AND (cont.dernier_jour_travaille IS NULL OR cont.dernier_jour_travaille >= CURRENT_DATE - INTERVAL '11 MONTH') -- à modifier avec les vraies valeurs
            AND vers.remuneration_nette_fiscale > 0
            AND (vers.date_versement >= CURRENT_DATE - INTERVAL '14 MONTH') -- à modifier avec les vraies valeurs
            AND ass.id = 171
            ORDER BY ass.id
            )
            SELECT
            ass_id,
            contrat_id,
            en_raison_sociale,
            nature_contrat,
            nature_contrat_libelle,
            date_debut_contrat,
            ROUND(AVG(montant_net_verse)) AS avg_remuneration_nette_fiscale_3M
            FROM A
            GROUP BY 1,2,3,4,5,6
            """;

    @Autowired
    private Environment env;
    @Autowired
    private DocumentFinancial documentFinancial;
    @Autowired
    private DocumentProfessional documentProfessional;
    @Autowired
    private TenantService tenantService;

    private static MultipartFile createEmptyMultipartFile() {
        // Créer un fichier vide
        File file = new File("infoContrat.pdf");

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Créer un objet MultipartFile vide avec le fichier vide
        MultipartFile multipartFile = null;
        multipartFile = new MockMultipartFile(
                "infoContrat.pdf",
                file.getName(),
                String.valueOf(ContentType.APPLICATION_PDF),
                new byte[10]);

        return multipartFile;
    }

    private DataSource dataSource() {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("hackathon.datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("hackathon.datasource.url"));
        dataSource.setUsername(env.getProperty("hackathon.datasource.username"));
        dataSource.setPassword(env.getProperty("hackathon.datasource.password"));

        return dataSource;
    }

    public List<DSNContract> contracts(String user) {
        List<DSNContract> contracts = new ArrayList<>();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        String sql = SELECT_2;
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer contractId = rs.getInt("contrat_id");
            String company = rs.getString("en_raison_sociale");
            String natureCode = rs.getString("nature_contrat");
            String natureContract = rs.getString("nature_contrat_libelle");


            String fechaStr = rs.getString("date_debut_contrat");
            SimpleDateFormat formatoInicial = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String debutContract = fechaStr;
            try {
                Date fecha = formatoInicial.parse(fechaStr);
                SimpleDateFormat formatoFinal = new SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE);
                debutContract = formatoFinal.format(fecha);
            } catch (ParseException e) {
                log.error("FORMAT");
            }
            Integer avgVersement = rs.getInt("avg_remuneration_nette_fiscale_3M");


            DSNContract c = DSNContract.builder().id(contractId).company(company)
                    .debut(debutContract)
                    .nature(natureContract)
                    .subCategory(DocumentSubCategory.getFromDSNNature(natureCode))
                    .averageAmount(avgVersement)
                    .payslips(
                            List.of(DSNPayslip.builder().amount(1001).period("Janvier 2023").build(),
                                    DSNPayslip.builder().amount(1304).period("Février 2023").build(),
                                    DSNPayslip.builder().amount(1101).period("Mars 2023").build())
                    )
                    .build();

            return c;
        }).forEach((c) -> {
            contracts.add(c);
            log.info("Add = " + c);
        });
        return contracts;
    }

    public void addRevenus() {

        Tenant tenant = tenantService.findByEmail("testUser@yopmail.com").get();

        List<DSNContract> contracts = contracts("testUser");

        for (DSNContract contract : contracts) {
            DocumentFinancialForm form = new DocumentFinancialForm();
            form.setMonthlySum(contract.getAverageAmount());
            form.setNoDocument(true);
            form.setTypeDocumentFinancial(contract.getSubCategory());
            form.setCustomText("Récupération des informations depuis DSN");
            form.setTypeDocumentFinancial(DocumentSubCategory.SALARY);

            documentFinancial.saveStep(tenant, form);
        }

    }

    public void addResource() {

        Tenant tenant = tenantService.findByEmail("testUser@yopmail.com").get();

        List<DSNContract> contracts = contracts("testUser");
        DSNContract contract = contracts.getFirst();

        DocumentProfessionalForm form = new DocumentProfessionalForm();
        form.setTypeDocumentProfessional(contract.getSubCategory());

        List<MultipartFile> docs = new ArrayList<>();
        docs.add(createEmptyMultipartFile());

        form.setDocuments(docs);
        documentProfessional.saveStep(tenant, form);


    }

}

