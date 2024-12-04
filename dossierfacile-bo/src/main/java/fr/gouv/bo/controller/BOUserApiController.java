package fr.gouv.bo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.service.KeycloakService;
import fr.gouv.bo.service.UserApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bo/userApi")
@Slf4j
public class BOUserApiController {
    private static final String REDIRECT_URL = "redirect:/bo/userApi";
    private final UserApiService userApiService;
    private final KeycloakService keycloakService;
    private final ObjectMapper mapper;

    @GetMapping("")
    public String index(Model model) {
        List<UserApi> userApiList = userApiService.findAll();
        model.addAttribute("userApiList", userApiList);
        model.addAttribute("userApiDTO", new UserApiDTO());
        return "bo/user-api";
    }

    @PostMapping("")
    public String create(@Validated @ModelAttribute("userApiDTO") UserApiDTO userApiDTO, BindingResult result) {
        if (result.hasErrors()) {
            log.error("BOUserApiController create has errors: {}", result.getAllErrors());
            return REDIRECT_URL;
        }
        userApiService.create(userApiDTO);
        return REDIRECT_URL;
    }

    @GetMapping("/{id}")
    public String updateForm(@PathVariable("id") Long id, Model model) {
        UserApi userApi = userApiService.findById(id);
        if (userApi == null) {
            log.error("BOUserApiController updateForm not found userApi with id : {}", id);
            return "redirect:/error";
        }
        UserApiDTO userApiDTO = mapper.convertValue(userApi, UserApiDTO.class);
        model.addAttribute("userApiDTO", userApiDTO);
        model.addAttribute("userApi", userApi);
        model.addAttribute("kcClient", keycloakService.getKeyCloakClient(userApi.getName()));
        return "bo/user-api-edit";
    }

    @PostMapping("/{id}/send")
    public String sendEmail(@PathVariable("id") Long id, @RequestParam("partnerEmail") String partnerEmail, Model model) {
        UserApi userApi = userApiService.findById(id);
        if (userApi == null) {
            log.error("BOUserApiController updateForm not found userApi with id : {}", id);
            return "redirect:/error";
        }
        userApiService.sendMailWithConfig(partnerEmail, userApi);
        return REDIRECT_URL;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") int id, @Validated @ModelAttribute("userApiDTO") UserApiDTO userApiDTO, BindingResult result) {
        if (result.hasErrors()) {
            log.error("BOUserApiController update has errors: {}", result.getAllErrors());
            return "bo/user-api-edit";
        }
        userApiService.save(userApiDTO);
        return REDIRECT_URL;
    }

}
