package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.TenantLog;
import fr.gouv.bo.dto.Pager;
import fr.gouv.bo.service.TenantLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping(value = "/bo/log")
public class BOLogController {

    private static final int BUTTONS_TO_SHOW = 5;
    private static final String INITIAL_PAGE = "1";
    private static final String INITIAL_PAGE_SIZE = "50";
    private static final int[] PAGE_SIZES = {50, 100};

    @Autowired
    private TenantLogService logService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("")
    public String index(Model model,
                        @RequestParam(value = "pageSize", defaultValue = INITIAL_PAGE_SIZE) int pageSize,
                        @RequestParam(value = "page", defaultValue = INITIAL_PAGE) int page,
                        @RequestParam("tenantId") Optional<String> tenantId) {

        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("creationDateTime").descending());

        Page<TenantLog> logs;
        if (tenantId.isPresent() && StringUtils.isNotBlank(tenantId.get())) {
            logs = logService.findAllByTenantIdPageable(
                    tenantId.map(Long::valueOf).orElseThrow(() -> new IllegalArgumentException("Tenant must be a number")),
                    pageable);
        } else {
            logs = logService.findAllPageable(pageable);
        }

        Pager pager = new Pager(logs.getTotalPages(), logs.getNumber(), BUTTONS_TO_SHOW);

        model.addAttribute("tenantId", tenantId.orElse(""));
        model.addAttribute("logs", logs);
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("selectedPageSize", pageable.getPageSize());
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pager", pager);

        return "bo/log";
    }
}
