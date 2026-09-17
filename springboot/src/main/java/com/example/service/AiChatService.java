package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.entity.AiModelConfig;
import com.example.exception.CustomException;
import com.example.mapper.AiModelConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大模型对话服务。
 * 读取【AI模型配置】中启用的模型（Base URL、API Key、模型名称），
 * 以 OpenAI 兼容的 /chat/completions 接口发起真实的大模型调用。
 * 只要是 OpenAI 兼容服务（DeepSeek、通义千问兼容模式、Kimi、智谱、OpenAI 等）都可直接使用，
 * 在页面上切换 Base URL 和模型名称即可，无需改动代码。
 */
@Service
public class AiChatService {

    @Resource
    private SpringAiModelFactory springAiModelFactory;

    @Resource
    private AiModelConfigMapper aiModelConfigMapper;

    /**
     * 读取当前启用的“对话模型”配置（供问答、推荐、对比、分析、报告、Agent 使用）。
     */
    public AiModelConfig resolveEnabledConfig() {
        return resolveEnabledConfig("CHAT");
    }

    /**
     * 按模型类型读取启用中的配置。没有或配置不完整时直接抛异常，避免用假数据糊弄用户。
     *
     * @param modelType CHAT 对话模型 / EMBEDDING 向量模型
     */
    public AiModelConfig resolveEnabledConfig(String modelType) {
        AiModelConfig config = findEnabledConfig(modelType);
        String label = "EMBEDDING".equals(modelType) ? "向量" : "对话";
        if (config == null) {
            throw new CustomException("500", "未配置启用的" + label + "模型，请先在【AI模型配置】中启用一个" + label + "模型（模型类型选" + label + "），并填写 Base URL、API Key 和模型名称");
        }
        return config;
    }

    /**
     * 查找启用中的指定类型模型配置；不存在或不完整时返回 null（调用方可选择回退）。
     */
    public AiModelConfig findEnabledConfig(String modelType) {
        // 用 is_enabled = 1 + model_type 作为查询条件，取第一条匹配的配置
        AiModelConfig condition = new AiModelConfig();
        condition.setIsEnabled(1);
        condition.setModelType(modelType);
        List<AiModelConfig> list = aiModelConfigMapper.selectAll(condition);
        if (list == null || list.isEmpty()) {
            return null;
        }
        AiModelConfig config = list.get(0);
        if (ObjectUtil.isEmpty(config.getBaseUrl())
                || ObjectUtil.isEmpty(config.getApiKey())
                || ObjectUtil.isEmpty(config.getModelName())) {
            return null;
        }
        return config;
    }

    /**
     * 使用当前启用的模型，按“系统提示词 + 用户提示词”发起一次对话，返回模型生成的文本。
     *
     * @param systemPrompt 系统提示词，约束模型角色与回答规则，可为空
     * @param userPrompt   用户提示词，通常包含检索到的资料上下文和用户的问题
     * @return 模型返回的回答文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        AiModelConfig config = resolveEnabledConfig();
        return chatWithConfig(config, systemPrompt, userPrompt);
    }

    /**
     * 用指定配置发起单轮对话。抽出单独方法，便于“商品工具调试”等场景直接传入配置测试。
     *
     * 这个方法走 Spring AI：把系统提示词和用户提示词交给 ChatClient，
     * 请求体拼装、鉴权头、响应解析都由 Spring AI 完成，不用自己拼 JSON。
     */
    public String chatWithConfig(AiModelConfig config, String systemPrompt, String userPrompt) {
        // 按数据库配置拿到（或构建）对应的 Spring AI 对话模型
        OpenAiChatModel chatModel = springAiModelFactory.getChatModel(config);
        try {
            ChatClient.ChatClientRequestSpec request = ChatClient.create(chatModel).prompt();
            if (StrUtil.isNotBlank(systemPrompt)) {
                request = request.system(systemPrompt);
            }
            String content = request.user(userPrompt).call().content();
            if (StrUtil.isBlank(content)) {
                throw new CustomException("500", "AI模型返回内容为空，请检查模型名和 API Key 是否正确");
            }
            return content;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            // Spring AI 会把 HTTP 错误包装成运行时异常，这里统一转成业务异常，保持原有的报错风格
            throw new CustomException("500", "调用对话模型失败：" + describeModelError(e));
        }
    }

    /**
     * 要求大模型以 JSON 返回，并解析成 JSONObject。
     * 自动去除模型常见的 ```json 代码围栏；解析失败时抛出异常，避免拿到脏数据继续处理。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词（内部应明确要求“只输出JSON”）
     * @return 解析后的 JSONObject
     */
    public JSONObject chatForJson(String systemPrompt, String userPrompt) {
        String content = chat(systemPrompt, userPrompt);
        String cleaned = stripJsonFences(content);
        try {
            return JSONUtil.parseObj(cleaned);
        } catch (Exception e) {
            throw new CustomException("500", "AI模型未按JSON格式返回：" + StrUtil.brief(content, 300));
        }
    }

    /**
     * 用指定的向量模型配置，把一段文本转成向量（OpenAI 兼容 /embeddings 接口）。
     * 供商品知识库切片向量化和检索时调用。
     *
     * @param config 向量模型配置（模型类型应为 EMBEDDING）
     * @param text   待向量化的文本
     * @return 浮点向量数组
     */
    public double[] embedWithConfig(AiModelConfig config, String text) {
        // 按数据库配置拿到（或构建）对应的 Spring AI 向量模型
        OpenAiEmbeddingModel embeddingModel = springAiModelFactory.getEmbeddingModel(config);
        try {
            // Spring AI 返回 float[]，而本项目的余弦相似度计算用 double[]，这里做一次转换。
            // 不直接改成 float 是为了不动上下游（向量存取、余弦计算、本地哈希回退）的签名。
            float[] vector = embeddingModel.embed(text);
            if (vector == null || vector.length == 0) {
                throw new CustomException("500", "向量模型返回内容为空，请检查向量模型名和 API Key 是否正确");
            }
            double[] result = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                result[i] = vector[i];
            }
            return result;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("500", "调用向量模型失败：" + describeModelError(e));
        }
    }

    /**
     * 底层对话补全接口，支持传入工具定义（Function Calling），返回助手消息 JSON。
     * 供“真 Agent”循环使用：模型可能返回 tool_calls，由调用方执行工具后回填结果再次调用。
     *
     * @param messages 完整的消息数组（system/user/assistant/tool）
     * @param tools    工具定义数组，可为空则表示普通对话
     * @return 助手消息 JSONObject（可能含 content 或 tool_calls）
     */
    public JSONObject chatCompletion(JSONArray messages, JSONArray tools) {
        AiModelConfig config = resolveEnabledConfig("CHAT");
        String url = buildChatUrl(config.getBaseUrl());
        JSONObject body = new JSONObject();
        body.set("model", config.getModelName());
        body.set("messages", messages);
        // 有工具则声明工具并让模型自主决定是否调用（tool_choice=auto）
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
            body.set("tool_choice", "auto");
        }
        if (config.getTemperature() != null) {
            body.set("temperature", config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            body.set("max_tokens", config.getMaxTokens());
        }
        body.set("stream", false);

        HttpResponse response;
        try {
            response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();
        } catch (Exception e) {
            throw new CustomException("500", "调用AI模型失败，请检查 Base URL 是否可访问：" + e.getMessage());
        }
        String responseBody = response.body();
        if (!response.isOk()) {
            throw new CustomException("500", "AI模型返回错误（HTTP " + response.getStatus() + "）：" + StrUtil.brief(responseBody, 300));
        }
        try {
            JSONObject json = JSONUtil.parseObj(responseBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new CustomException("500", "AI模型返回内容为空：" + StrUtil.brief(responseBody, 300));
            }
            return choices.getJSONObject(0).getJSONObject("message");
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("500", "解析AI模型返回失败：" + e.getMessage());
        }
    }

    /**
     * 把模型服务端的报错翻译成一句使用者能看懂、知道该怎么处理的提示。
     *
     * 模型服务失败时返回的是一大段 JSON（包含 error.code、request_id 等），
     * 直接抵到页面上用户看不懂，所以这里识别几种最常见的错误先给出结论。
     */
    /**
     * 判断这个错误是不是"重试也没用"的类型。
     *
     * 欠费、Key 无效、模型名写错这三类属于账户或配置问题，不会因为多试几次就好。
     * 批量生成靠它判断要不要提前停下来，避免对几十个切片重复发注定失败的请求。
     */
    public boolean isFatalModelError(Exception e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        return raw.contains("Arrearage")
                || raw.contains("insufficient_quota")
                || raw.contains("overdue")
                || raw.contains("Invalid API-key")
                || raw.contains("invalid_api_key")
                || raw.contains("model_not_found")
                || raw.contains("Model not exist");
    }

    public String describeModelError(Exception e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        // 阿里百炼用 Arrearage 表示账户欠费，OpenAI 系用 insufficient_quota 表示额度用完
        if (raw.contains("Arrearage") || raw.contains("insufficient_quota") || raw.contains("overdue")) {
            return "模型平台账户欠费或免费额度已用完，请先到平台充值，充值后无需修改配置即可继续使用";
        }
        if (raw.contains("Invalid API-key") || raw.contains("invalid_api_key") || raw.contains("401")) {
            return "API Key 无效，请到【AI模型配置】检查密钥是否填错或已失效";
        }
        if (raw.contains("model_not_found") || raw.contains("Model not exist")) {
            return "模型名不存在，请到【AI模型配置】核对模型名称是否和平台一致";
        }
        if (raw.contains("429") || raw.contains("rate_limit") || raw.contains("Throttling")) {
            return "请求太频繁被平台限流，请稍后重试";
        }
        if (raw.contains("Connection") || raw.contains("timed out") || raw.contains("timeout")) {
            return "连不上模型服务，请检查网络和【AI模型配置】里的 Base URL";
        }
        // 没匹配到已知类型时，截断后原文返回，保留排查线索
        return StrUtil.maxLength(raw, 200);
    }

    /**
     * 去掉模型返回里包裹 JSON 的 ```json ... ``` 代码围栏，只留纯 JSON 文本。
     */
    private String stripJsonFences(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            // 去掉第一行的 ``` 或 ```json
            int newline = text.indexOf('\n');
            if (newline >= 0) {
                text = text.substring(newline + 1);
            }
            // 去掉结尾的 ```
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.trim();
    }

    /**
     * 把用户填写的 Base URL 规范成 chat/completions 地址。
     * 兼容三种写法：已是完整地址(.../chat/completions)、以 / 结尾、普通基础地址(.../v1)。
     */
    private String buildChatUrl(String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + "/chat/completions";
    }
}
