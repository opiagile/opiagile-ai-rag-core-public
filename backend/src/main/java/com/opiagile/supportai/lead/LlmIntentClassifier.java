package com.opiagile.supportai.lead;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "intent.llm.enabled", havingValue = "true")
public class LlmIntentClassifier implements IntentClassifier {

    @Override
    public Intent classify(String message) {
        return Intent.DESCONHECIDO;
    }
}
