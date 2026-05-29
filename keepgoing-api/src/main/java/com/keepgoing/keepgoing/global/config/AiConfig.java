package com.keepgoing.keepgoing.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

	@Bean
	ChatClient aiPanelChatClient(
			ChatClient.Builder builder,
			@Value("${spring.ai.logging.prompt-response.enabled:false}") boolean promptLogging
	) {
		ChatClient.Builder configured = builder
				.defaultSystem("""
						 너는 노트 서비스 안에서 동작하는 AI 패널이다.
						 사용자의 질문에 간결하고 실용적으로 답변한다.
						 note context가 주어지면 참고하되, 실제 저장/수정이 완료된 것처럼 말하지 않는다.
						 필요 하면 수정 방향이나 제안 형태로 답한다.
						""");
		if (promptLogging) {
			configured.defaultAdvisors(new SimpleLoggerAdvisor());
		}

		return configured.build();
	}
}
