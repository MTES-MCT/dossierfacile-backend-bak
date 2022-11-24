package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.repository.TenantRepository;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProcessTenantIT {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private DocumentRepository documentRepository;

    @MockBean
    private ProcessTaxDocument processTaxDocument;

    @Autowired
    private ProcessTenantImpl processTenant;

    @ParameterizedTest
    @NullAndEmptySource
    void should_not_process_file_if_tenant_is_nameless(String emptyName) {
        Tenant tenant = givenTenant(emptyName, emptyName);

        processTenant.process(tenant.getId());

        verifyNoInteractions(processTaxDocument);
    }

    @Test
    void should_process_tax_documents() {
        Tenant tenant = givenTenant("John", "Doe");
        givenDocumentOf(tenant, DocumentCategory.FINANCIAL);
        Document taxDocument = givenDocumentOf(tenant, DocumentCategory.TAX);

        processTenant.process(tenant.getId());

        assertThatOnlyThisDocumentIsProcessed(taxDocument);
    }

    private void assertThatOnlyThisDocumentIsProcessed(Document taxDocument) {
        verify(processTaxDocument, times(1)).process(eq(taxDocument), any(Tenant.class));
        verify(processTaxDocument, never()).process(any(), any(Guarantor.class));
    }

    private Tenant givenTenant(String firstName, String lastName) {
        Tenant tenant = new Tenant();
        tenant.setFirstName(firstName);
        tenant.setLastName(lastName);
        return tenantRepository.save(tenant);
    }

    private Document givenDocumentOf(Tenant tenant, DocumentCategory documentCategory) {
        Document document = Document.builder()
                .documentCategory(documentCategory)
                .noDocument(false)
                .tenant(tenant)
                .build();
        return documentRepository.save(document);
    }

}