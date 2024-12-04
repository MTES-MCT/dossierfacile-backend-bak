package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.gouv.bo.dto.DocumentDeniedOptionsDTO;
import fr.gouv.bo.service.DocumentDeniedOptionsService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

@Controller
@AllArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequestMapping("/bo/documentDeniedOptions")
public class BODocumentDeniedOptionsController {

    private static final String EMAIL = "email";
    private static final List<String> DOCUMENT_USER_TYPES = List.of("tenant", "guarantor");

    private final DocumentDeniedOptionsService service;

    @GetMapping
    public String documentDeniedOptions(Model model,
                                        @RequestParam(value = "documentSubCategory", required = false) String documentSubCategory) {
        List<DocumentDeniedOptions> documentDeniedOptions = service.findDocumentDeniedOptions(documentSubCategory);
        documentDeniedOptions.sort(comparing(DocumentDeniedOptions::getCode));

        model.addAttribute("documentDeniedOptions", documentDeniedOptions);
        model.addAttribute("documentSubCategories", DocumentSubCategory.alphabeticallySortedValues());

        return "bo/document-denied-options";
    }

    @GetMapping("/{id}")
    public String editDocumentDeniedOption(Model model,
                                           @PathVariable(value = "id") int id) {
        Optional<DocumentDeniedOptions> optionToEdit = service.findDocumentDeniedOption(id);
        if (optionToEdit.isEmpty()) {
            return "redirect:/bo/documentDeniedOptions";
        }
        model.addAttribute("documentDeniedOption", optionToEdit.get());
        return "bo/edit-document-denied-option";
    }

    @GetMapping("/create")
    public String createDocumentDeniedOption(Model model) {
        model.addAttribute("documentSubCategories", DocumentSubCategory.alphabeticallySortedValues());
        model.addAttribute("documentUserTypes", DOCUMENT_USER_TYPES);
        model.addAttribute("documentDeniedOption", new DocumentDeniedOptionsDTO());
        return "bo/create-document-denied-option";
    }

    @PostMapping
    public String saveNewDocumentDeniedOption(@ModelAttribute("documentDeniedOption") DocumentDeniedOptionsDTO createdOption) {
        service.createDocumentDeniedOption(createdOption);
        return "redirect:/bo/documentDeniedOptions";
    }

    @PostMapping("/{id}")
    public String saveDocumentDeniedOption(@PathVariable(value = "id") int id,
                                           @ModelAttribute("documentDeniedOption") DocumentDeniedOptionsDTO modifiedOption) {
        service.updateMessage(id, modifiedOption.getMessageValue());
        return "redirect:/bo/documentDeniedOptions";
    }

    @DeleteMapping("/{id}")
    public String deleteDocumentDeniedOption(@PathVariable(value = "id") int id) {
        service.deleteDocumentDeniedOption(id);
        return "redirect:/bo/documentDeniedOptions";
    }

}
