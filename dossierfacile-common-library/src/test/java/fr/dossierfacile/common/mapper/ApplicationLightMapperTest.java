package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;

class ApplicationLightMapperTest implements DocumentMappingTest, AuthenticityStatusMappingTest {

    @Override
    public DocumentModel mapDocument(Document document) {
        ApplicationLightMapperImpl mapper = new ApplicationLightMapperImpl();
        mapper.setSubCategoryMapper(new DummySubCategoryMapper());
        return mapper.documentToDocumentModel(document);
    }

}