package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.entity.AiModelConfig;
import com.example.exception.CustomException;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按数据库里的模型配置构建 Spring AI 的模型对象。
 *
 * Spring AI 的常规用法是把模型信息写在 application.yml 里，由自动配置生成一个 ChatModel Bean。
 * 但本项目的模型配置存在 ai_model_config 表里，管理员在后台改完就要立刻生效、不能重启，
 * 所以这里不用自动配置的 Bean，而是自己按配置构建，并把构建结果缓存起来复用。
 */
@Component
public class SpringAiModelFactory {

    // 缓存已构建的对话模型，key 由配置内容拼成，配置变了 key 就变，自然拿到新实例
    private final Map<String, OpenAiChatModel> chatModelCache = new ConcurrentHashMap<>();
    // 缓存已构建的向量模型
    private final Map<String, OpenAiEmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /**
     * 取（或构建）一个对话模型。
     */
    public OpenAiChatModel getChatModel(AiModelConfig config) {
        validate(config);
        return chatModelCache.computeIfAbsent(cacheKey(config), key -> buildChatModel(config));
    }

    /**
     * 取（或构建）一个向量模型。
     */
    public OpenAiEmbeddingModel getEmbeddingModel(AiModelConfig config) {
        validate(config);
        return embeddingModelCache.computeIfAbsent(cacheKey(config), key -> buildEmbeddingModel(config));
    }

    private OpenAiChatModel buildChatModel(AiModelConfig config) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(config.getModelName());
        // temperature 和 maxTokens 在表里允许为空，为空就用模型服务端的默认值
        if (config.getTemperature() != null) {
            optionsBuilder.temperature(config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.maxTokens(config.getMaxTokens());
        }
        return OpenAiChatModel.builder()
                .openAiApi(buildApi(config))
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    private OpenAiEmbeddingModel buildEmbeddingModel(AiModelConfig config) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(config.getModelName())
                .build();
        // MetadataMode.NONE：只把文本本身向量化，不把 Document 的元数据拼进去
        return new OpenAiEmbeddingModel(buildApi(config), MetadataMode.NONE, options);
    }

    /**
     * 构造 OpenAiApi。
     * 表里的 base_url 允许写两种形式：写到 /v1 的短地址，或者带完整路径的长地址。
     * OpenAiApi 需要的是"主机 + 路径"分开的形式，所以这里把它拆开。
     */
    private OpenAiApi buildApi(AiModelConfig config) {
        String rawUrl = config.getBaseUrl().trim();
        String baseUrl = rawUrl;
        String completionsPath = "/v1/chat/completions";
        String embeddingsPath = "/v1/embeddings";

        int pathIndex = rawUrl.indexOf("/v1");
        if (pathIndex > 0) {
            // 把 /v1 及其后面的部分当作路径，前面的部分当作主机
            baseUrl = rawUrl.substring(0, pathIndex);
            String path = rawUrl.substring(pathIndex);
            if (path.endsWith("/chat/completions")) {
                completionsPath = path;
                embeddingsPath = path.replace("/chat/completions", "/embeddings");
            } else if (path.endsWith("/embeddings")) {
                embeddingsPath = path;
                completionsPath = path.replace("/embeddings", "/chat/completions");
            } else {
                // 只写到 /v1 或 /compatible-mode/v1 这种，自己补齐两个路径
                completionsPath = StrUtil.removeSuffix(path, "/") + "/chat/completions";
                embeddingsPath = StrUtil.removeSuffix(path, "/") + "/embeddings";
            }
        }

        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(config.getApiKey())
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .build();
    }

    /**
     * 缓存 key 用配置内容拼，而不是用主键 ID。
     * 这样管理员改了 Key 或模型名之后 key 会变，下次调用自动构建新实例，不会命中旧配置。
     */
    private String cacheKey(AiModelConfig config) {
        return config.getBaseUrl() + "|" + config.getApiKey() + "|" + config.getModelName()
                + "|" + config.getTemperature() + "|" + config.getMaxTokens();
    }

    private void validate(AiModelConfig config) {
        if (config == null
                || StrUtil.isBlank(config.getBaseUrl())
                || StrUtil.isBlank(config.getApiKey())
                || StrUtil.isBlank(config.getModelName())) {
            throw new CustomException("500", "请先在【AI模型配置】里维护可用的模型（Base URL、API Key、模型名都要填）");
        }
    }
}
