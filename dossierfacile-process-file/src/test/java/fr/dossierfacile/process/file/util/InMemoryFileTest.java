package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.barcode.InMemoryFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryFileTest {

    private static File fileWithPath(String path) {
        return File.builder()
                .storageFile(StorageFile.builder()
                        .path(path)
                        .contentType("application/pdf")
                        .build())
                .build();
    }

    @Test
    void file_with_qr_code() throws IOException {
        File file = fileWithPath("fake-payfit.pdf");

        InMemoryFile inMemoryPdfFile = InMemoryFile.download(file, classpathStorageService());

        assertThat(inMemoryPdfFile.hasQrCode()).isTrue();
        assertThat(inMemoryPdfFile.getContentAsString()).isNotEmpty();
    }

    @Test
    void file_with_2DDoc() throws IOException {
        File file = fileWithPath("2ddoc.pdf");

        InMemoryFile inMemoryPdfFile = InMemoryFile.download(file, classpathStorageService());

        assertThat(inMemoryPdfFile.has2DDoc()).isTrue();
    }

    @Test
    void file_with_only_text() throws IOException {
        File file = fileWithPath("test-document.pdf");

        InMemoryFile inMemoryPdfFile = InMemoryFile.download(file, classpathStorageService());

        assertThat(inMemoryPdfFile.hasQrCode()).isFalse();
        assertThat(inMemoryPdfFile.has2DDoc()).isFalse();
        assertThat(inMemoryPdfFile.getContentAsString()).isNotEmpty();
        assertThat(inMemoryPdfFile.getContentAsString().trim()).isEqualTo("Test document");
    }

    private FileStorageService classpathStorageService() throws IOException {
        FileStorageService fileStorageService = mock(FileStorageService.class);
        when(fileStorageService.download(any(StorageFile.class))).thenAnswer(invocation -> {
            StorageFile file = invocation.getArgument(0, StorageFile.class);
            return TestFilesUtil.getFileAsStream(file.getPath());
        });
        return fileStorageService;
    }

}