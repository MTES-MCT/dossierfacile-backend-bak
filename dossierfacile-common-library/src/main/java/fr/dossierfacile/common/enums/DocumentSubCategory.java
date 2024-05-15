package fr.dossierfacile.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum DocumentSubCategory {

    // Identification
    FRENCH_IDENTITY_CARD("document_sub_category.FRENCH_IDENTITY_CARD"),
    FRENCH_PASSPORT("document_sub_category.FRENCH_PASSPORT"),
    FRENCH_RESIDENCE_PERMIT("document_sub_category.FRENCH_RESIDENCE_PERMIT"),
    DRIVERS_LICENSE("document_sub_category.DRIVERS_LICENSE"),
    FRANCE_IDENTITE("document_sub_category.FRANCE_IDENTITE"),
    OTHER_IDENTIFICATION("document_sub_category.OTHER_IDENTIFICATION"),

    // Residency
    TENANT("document_sub_category.TENANT"),
    OWNER("document_sub_category.OWNER"),
    GUEST_PARENTS("document_sub_category.GUEST_PARENTS"),
    GUEST("document_sub_category.GUEST"),
    GUEST_ORGANISM("document_sub_category.GUEST_ORGANISM"),
    SHORT_TERM_RENTAL("document_sub_category.SHORT_TERM_RENTAL"),
    OTHER_RESIDENCY("document_sub_category.OTHER_RESIDENCY"),

    // Professional
    CDI("document_sub_category.CDI"),
    CDI_TRIAL("document_sub_category.CDI_TRIAL"),
    CDD("document_sub_category.CDD"),
    ALTERNATION("document_sub_category.ALTERNATION"),
    INTERNSHIP("document_sub_category.INTERNSHIP"),
    STUDENT("document_sub_category.STUDENT"),
    PUBLIC("document_sub_category.PUBLIC"),
    CTT("document_sub_category.CTT"),
    RETIRED("document_sub_category.RETIRED"),
    UNEMPLOYED("document_sub_category.UNEMPLOYED"),
    INDEPENDENT("document_sub_category.INDEPENDENT"),
    INTERMITTENT("document_sub_category.INTERMITTENT"),
    STAY_AT_HOME_PARENT("document_sub_category.STAY_AT_HOME_PARENT"),
    NO_ACTIVITY("document_sub_category.NO_ACTIVITY"),
    ARTIST("document_sub_category.ARTIST"),
    OTHER("document_sub_category.OTHER"),

    // Financial
    SALARY("document_sub_category.SALARY"),
    SCHOLARSHIP("document_sub_category.SCHOLARSHIP"),
    SOCIAL_SERVICE("document_sub_category.SOCIAL_SERVICE"),
    RENT("document_sub_category.RENT"),
    PENSION("document_sub_category.PENSION"),
    NO_INCOME("document_sub_category.NO_INCOME"),

    // Tax
    MY_NAME("document_sub_category.MY_NAME"),
    MY_PARENTS("document_sub_category.MY_PARENTS"),
    LESS_THAN_YEAR("document_sub_category.LESS_THAN_YEAR"),
    OTHER_TAX("document_sub_category.OTHER_TAX"),

    // Guarantor
    CERTIFICATE_VISA("document_sub_category.CERTIFICATE_VISA"),
    VISALE("document_sub_category.VISALE"),
    OTHER_GUARANTEE("document_sub_category.OTHER_GUARANTEE"),
    LEGAL_PERSON("document_sub_category.LEGAL_PERSON"),
    OTHER_PROFESSION_GUARANTOR("document_sub_category.OTHER_PROFESSION_GUARANTOR"),

    UNDEFINED("document_sub_category.UNDEFINED");

    final String label;

    DocumentSubCategory(String label) {
        this.label = label;
    }

    public static List<DocumentSubCategory> alphabeticallySortedValues() {
        List<DocumentSubCategory> values = Arrays.asList(values());
        values.sort(Comparator.comparing(DocumentSubCategory::name));
        return values;
    }

    public static DocumentSubCategory getFromDSNNature(String code){
        return switch(code){
            case "01" -> CDI;
            case "02" -> CDD;
            case "03" -> CTT;
            case "07" -> INTERMITTENT;
            case "08" -> CTT;
            case "09", "10" -> PUBLIC;
            default -> OTHER;
        };

    }

}
