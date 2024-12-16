package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceImplTest {
    {
        MockitoAnnotations.openMocks(this);
        Mockito.lenient();
    }

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private Producer producer;
    @InjectMocks
    private DocumentServiceImpl documentService;

    public DocumentServiceImplTest() {
        MockitoAnnotations.openMocks(this);
        Mockito.lenient();
    }

    @Test
    void testResetValidatedOrInProgressDocumentsForValidatedDocument() {
        List<Document> documentList = new ArrayList<>();
        Document document = new Document();
        document.setId(1L);
        document.setDocumentCategory(DocumentCategory.PROFESSIONAL);
        document.setDocumentStatus(DocumentStatus.VALIDATED);
        documentList.add(document);

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL);

        when(documentRepository.save(any(Document.class))).thenReturn(document);

        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);

        assertEquals(DocumentStatus.TO_PROCESS, document.getDocumentStatus());
        assertNull(document.getDocumentDeniedReasons());
    }

    @Test
    void testResetValidatedOrInProgressDocumentsForNoDocumentWithWatermark() {
        List<Document> documentList = new ArrayList<>();
        Document document = new Document();
        document.setId(1L);
        document.setDocumentCategory(DocumentCategory.PROFESSIONAL);
        document.setDocumentStatus(DocumentStatus.VALIDATED);
        document.setNoDocument(true);
        document.setWatermarkFile(null);
        documentList.add(document);

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL);

        when(documentRepository.save(any(Document.class))).thenReturn(document);

        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);

        assertEquals(DocumentStatus.TO_PROCESS, document.getDocumentStatus());
        assertNull(document.getWatermarkFile());

    }

    @Test
    void testResetValidatedOrInProgressDocumentsForNonMatchingCategory() {
        List<Document> documentList = new ArrayList<>();
        Document document = new Document();
        document.setId(1L);
        document.setDocumentCategory(DocumentCategory.IDENTIFICATION);
        document.setDocumentStatus(DocumentStatus.VALIDATED);
        documentList.add(document);

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL);

        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);

        verifyNoInteractions(documentRepository);
        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(producer);
        assertEquals(DocumentStatus.VALIDATED, document.getDocumentStatus());
    }

    @Test
    void testResetValidatedOrInProgressDocumentsForEmptyList() {
        List<Document> documentList = new ArrayList<>();
        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL);

        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);

        verifyNoInteractions(documentRepository);
        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(producer);
        verifyNoMoreInteractions(documentRepository, fileStorageService, producer);
    }

    @Test
    void testResetValidatedOrInProgressDocumentsForNullList() {
        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL);

        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(null, categoriesToChange);

        verifyNoInteractions(documentRepository);
        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(producer);
        verifyNoMoreInteractions(documentRepository, fileStorageService, producer);
    }
}