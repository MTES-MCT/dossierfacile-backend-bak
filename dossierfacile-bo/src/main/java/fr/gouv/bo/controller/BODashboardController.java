package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping(value = "/bo/dashboard")
public class BODashboardController {
    public final String EMAIL = "email";
    @Autowired
    private TenantLogService logService;
    @Autowired
    private UserService userService;
    @Value("${dashboard.days.before.to.display:5}")
    private Integer minusDays;

    @GetMapping("")
    public String myDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        BOUser operator = userService.findUserByEmail(principal.getName());
        List<Object[]> listTreatedCountByDay = logService.listLastTreatedFilesByOperator(operator.getId(), minusDays);

        model.addAttribute("operator", operator);
        model.addAttribute("listTreatedCountByDay", listTreatedCountByDay);

        return "bo/dashboard";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public String boUserDashboard(Model model) {
        List<Object[]> listTreatedCountByOperator = logService.listDailyTreatedFilesByOperator();
        int dailyCount = listTreatedCountByOperator.stream()
                .mapToInt(objects -> ((Number) objects[1]).intValue())
                .sum();

        model.addAttribute("listTreatedCountByOperator", listTreatedCountByOperator);
        model.addAttribute("dailyCount", dailyCount);

        return "bo/dashboard-admin";
    }
}
