package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BOUserController {
    private final UserService userService;

    @GetMapping("/bo/users")
    public String getBOUser(Model model) {
        model.addAttribute("users", userService.findAll());
        return "bo/users";
    }

    @PostMapping("/bo/users")
    public String createBOUser(EmailDTO emailDTO, Model model, @RequestParam(defaultValue = "ROLE_OPERATOR", name = "action") Role role) {
        BOUser user = userService.findUserByEmail(emailDTO.getEmail());
        if (user == null) {
            userService.createUserByEmail(emailDTO.getEmail(), role);
        } else {
            throw new IllegalArgumentException("Utilisateur existe déjà");
        }

        return "redirect:/bo/users";
    }

    @DeleteMapping("/bo/users/{email}/roles/{role}")
    public String deleteRole(Model model, @PathVariable(name = "email") String email, @PathVariable(name = "role") Role role) {
        BOUser user = userService.findUserByEmail(email);
        userService.deleteRoles(user, Collections.singletonList(role));
        return "redirect:/bo/users";
    }

    @PostMapping("/bo/users/{email}/roles")
    public String addRole(@RequestParam(name = "role") Role role, Model model, @PathVariable(name = "email") String email) {
        BOUser user = userService.findUserByEmail(email);
        userService.addRoles(user, Collections.singletonList(role));
        return "redirect:/bo/users";
    }
}
