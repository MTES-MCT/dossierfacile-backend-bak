package fr.gouv.bo.controller;

import fr.gouv.bo.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class BOToolsController {
    private final TenantService tenantService;

    @GetMapping("/bo/tools")
    public String outils(Model model) {
        return "bo/tools";
    }

    /**
     * Emails list split by ","
     */
    @PostMapping(value = "/bo/tools/regenerateStatusOfTenants")
    public String regenerateStatusOfTenants(@RequestParam("tenantList") String tenantList, Model model) {
        try {
            tenantService.updateStatusOfSomeTenants(tenantList);
        } catch (Exception e) {
            log.error("Error regenerating status for tenants", e);
            model.addAttribute("errorMessage", "Quelque chose c'est mal passé: " + e.getMessage());
        }
        return "bo/tools";
    }

}