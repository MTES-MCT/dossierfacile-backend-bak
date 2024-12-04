package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ProcessingCapacity;
import fr.dossierfacile.common.repository.ProcessingCapacityRepository;
import fr.gouv.bo.dto.ProcessCapacitiesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bo/admin/process")
@Slf4j
public class BOProcessDossierController {

    private final ProcessingCapacityRepository processingCapacityRepository;

    @GetMapping("/capacities")
    public String index(Model model) {
        List<ProcessingCapacity> capacities = new ArrayList<>(14);
        LocalDate date = LocalDate.now();
        for (int i = 0; i < 14; i++, date = date.plusDays(1)) {
            ProcessingCapacity dailyCapacity = processingCapacityRepository.findByDate(date);
            if (dailyCapacity == null) {
                dailyCapacity = ProcessingCapacity.builder()
                        .date(date)
                        .dailyCount(0)
                        .build();
            }
            capacities.add(dailyCapacity);
        }
        ProcessCapacitiesDTO pcDto = ProcessCapacitiesDTO.builder().list(capacities).build();

        model.addAttribute("processCapacitiesDTO", pcDto);
        return "bo/process-capacities";
    }

    @PostMapping("/capacities")
    public String create(@ModelAttribute("processCapacitiesDTO") ProcessCapacitiesDTO capacitiesDto, BindingResult result) {
        if (result.hasErrors()) {
            log.error("Failed to update processing capacities - POST has errors: {}", result.getAllErrors());
        }
        processingCapacityRepository.saveAll(capacitiesDto.getList());
        return "redirect:/bo/admin/process/capacities";
    }

}
