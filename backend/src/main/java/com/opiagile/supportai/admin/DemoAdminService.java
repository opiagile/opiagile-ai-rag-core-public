package com.opiagile.supportai.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoAdminService {

    private final JdbcClient jdbc;

    public DemoAdminService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public DemoResetResponse resetDemoData() {
        int whatsappEvents = deleteFrom("whatsapp_webhook_events");
        int retrievalLogs = deleteFrom("retrieval_logs");
        int handoffs = deleteFrom("handoff_requests");
        int leads = deleteFrom("leads");
        int messages = deleteFrom("messages");
        int conversations = deleteFrom("conversations");
        int chunks = deleteFrom("document_chunks");
        int documents = deleteFrom("documents");
        return new DemoResetResponse(whatsappEvents, retrievalLogs, handoffs, leads, messages, conversations, chunks, documents);
    }

    private int deleteFrom(String table) {
        return jdbc.sql("DELETE FROM " + table).update();
    }
}
