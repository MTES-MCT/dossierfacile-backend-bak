package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DSNContract {
    private Integer id;
    private String company;
    private String debut;
    private String nature;
    private Integer averageAmount;
    private DocumentSubCategory subCategory = DocumentSubCategory.CDI;
    private DocumentSubCategory subCategoryFinancial = DocumentSubCategory.SALARY;
    private List<DSNPayslip> payslips;
}
