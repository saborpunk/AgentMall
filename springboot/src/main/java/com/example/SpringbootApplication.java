package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 这里排除了 Spring AI 的 6 个 OpenAI 自动配置。
 *
 * 原因：这些自动配置要从 application.yml 的 spring.ai.openai.api-key 读密钥，读不到就直接启动失败。
 * 而本项目的模型配置（Base URL、API Key、模型名）存在 ai_model_config 表里，由管理员在后台维护，
 * 模型对象统一由 SpringAiModelFactory 按数据库配置手动构建，用不到这些自动配置的 Bean。
 * 不排除的话，只要没在配置文件里填 Key，整个后端都起不来。
 */
@SpringBootApplication(exclude = {
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class
})
@MapperScan("com.example.mapper")
@EnableAsync
public class SpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class, args);
    }

}
