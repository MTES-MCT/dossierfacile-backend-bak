package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.repository.GuarantorRepository;
import fr.dossierfacile.api.pdfgenerator.repository.TenantRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.api.pdfgenerator.util.Fonts;
import fr.dossierfacile.api.pdfgenerator.util.Utility;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static fr.dossierfacile.api.pdfgenerator.service.templates.PdfFileTemplate.DOCUMENT_FINANCIAL;
import static fr.dossierfacile.api.pdfgenerator.service.templates.PdfFileTemplate.DOCUMENT_TAX;

@Service
@AllArgsConstructor
@Slf4j
public class EmptyBOPdfDocumentTemplate implements PdfTemplate<Document> {

    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;
    private final TenantRepository tenantRepository;
    private final GuarantorRepository guarantorRepository;

    @Override
    public InputStream render(Document document) throws IOException {
        return new ByteArrayInputStream(createPdfFromTemplate(document).toByteArray());
    }

    private ByteArrayOutputStream createPdfFromTemplate(Document document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfFileTemplate pdfTemplate;

        PdfTextElements textElements = new PdfTextElements(getPersonName(document));
        if (document.getDocumentCategory() == DocumentCategory.FINANCIAL) {
            pdfTemplate = DOCUMENT_FINANCIAL;
            textElements.addTextToHeader(messageSource.getMessage("tenant.document.financial.justification.nodocument", null, locale));
            textElements.addExplanation(document.getCustomText());
        } else { //DocumentCategory.TAX
            pdfTemplate = DOCUMENT_TAX;
            textElements.addTextToHeader(messageSource.getMessage("tenant.document.tax.justification.nodocument", null, locale));
            if (document.getDocumentSubCategory() == DocumentSubCategory.MY_PARENTS) {
                textElements.addExplanation(messageSource.getMessage("tenant.document.tax.justification.parents", null, locale));
            } else if (document.getDocumentSubCategory() == DocumentSubCategory.LESS_THAN_YEAR) {
                textElements.addExplanation(messageSource.getMessage("tenant.document.tax.justification.less_than_year", null, locale));
            } else { //DocumentSubCategory.OTHER_TAX
                textElements.addExplanation(document.getCustomText());
            }
        }

        try (PDDocument pdDocument = PDDocument.load(pdfTemplate.getInputStream())) {

            PDType0Font font = Fonts.ARIAL_NOVA_LIGHT.load(pdDocument);
            PDType0Font alternativeFont = Fonts.NOTO_EMOJI_MEDIUM.load(pdDocument);
            PDPage pdPage = pdDocument.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, true, true);
            contentStream.setNonStrokingColor(74 / 255.0F, 144 / 255.0F, 226 / 255.0F);
            float fontSize = 11;
            float leading = 1.5f * fontSize;
            float marginY = 360;
            float marginX = 60;
            PDRectangle mediaBox = pdPage.getMediaBox();
            float width = mediaBox.getWidth() - 2 * marginX;
            float startX = mediaBox.getLowerLeftX() + marginX;
            float startY = mediaBox.getUpperRightY() - marginY;

            contentStream.setLeading(leading);

            Utility.addText(contentStream, width, startX, startY, textElements.header, font, fontSize, alternativeFont);
            Utility.addText(contentStream, width, startX, startY - 36, textElements.explanation, font, fontSize, alternativeFont);

            contentStream.close();
            pdDocument.save(outputStream);

        } catch (IOException e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            throw e;
        }
        return outputStream;
    }

    private String getPersonName(Document document) {
        Long documentId = document.getId();
        return tenantRepository.getTenantByDocumentId(documentId)
                .map(Tenant::getFullName)
                .orElseGet(() -> guarantorRepository.getGuarantorByDocumentId(documentId)
                        .map(Guarantor::getCompleteName)
                        .orElse("")
                );
    }

    private static final class PdfTextElements {

        private String header;
        private String explanation;

        public PdfTextElements(String personName) {
            header = StringUtils.normalizeSpace(personName) + " ";
        }

        void addTextToHeader(String text) {
            header += StringUtils.normalizeSpace(text);
        }

        void addExplanation(String text) {
            explanation = StringUtils.normalizeSpace(text);
        }

    }

}
