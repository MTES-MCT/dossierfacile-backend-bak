package fr.dossierfacile.common.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DSNPayslip {
    private Integer amount;
    private String period;

}
