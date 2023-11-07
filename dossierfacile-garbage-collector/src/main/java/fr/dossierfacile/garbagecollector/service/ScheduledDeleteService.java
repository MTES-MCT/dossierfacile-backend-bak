package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.StorageFileToDelete;
import fr.dossierfacile.common.repository.StorageFileToDeleteRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageToDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDeleteService {

    private final StorageFileToDeleteRepository storageFileToDeleteRepository;
    private final FileStorageToDeleteService fileStorageToDeleteService;

    @Scheduled(fixedDelay = 10000)
    public void deleteFileInProviderTask() {
        List<StorageFileToDelete> storageFileToDeleteList = storageFileToDeleteRepository.findAll();
        for (StorageFileToDelete storageFileToDelete : storageFileToDeleteList) {
            fileStorageToDeleteService.delete(storageFileToDelete);
        }
    }

}
