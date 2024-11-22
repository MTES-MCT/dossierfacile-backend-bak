package fr.gouv.bo.amqp;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final QueueMessageRepository queueMessageRepository;

    @Async
    public void generatePdf(Long documentId) {
        log.debug("Sending document with ID [{}] for pdf generation", documentId);
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_DOCUMENT_WATERMARK_PDF)
                .documentId(documentId)
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }

}
