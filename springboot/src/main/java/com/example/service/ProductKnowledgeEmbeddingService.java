package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AiModelConfig;
import com.example.entity.EmbeddingGenerateProgress;
import com.example.entity.EmbeddingSearchRequest;
import com.example.entity.Product;
import com.example.entity.ProductKnowledgeChunk;
import com.example.entity.ProductKnowledgeEmbedding;
import com.example.exception.CustomException;
import com.example.mapper.ProductKnowledgeChunkMapper;
import com.example.mapper.ProductKnowledgeEmbeddingMapper;
import com.example.mapper.ProductMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProductKnowledgeEmbeddingService {

    // 未配置向量模型时的本地回退方案：本地哈希向量，固定 64 维
    private static final String LOCAL_EMBEDDING_MODEL = "local-hash-embedding-v1";
    private static final int LOCAL_VECTOR_DIMENSION = 64;

    @Resource
    private ProductKnowledgeChunkMapper productKnowledgeChunkMapper;
    @Resource
    private ProductKnowledgeEmbeddingMapper productKnowledgeEmbeddingMapper;
    @Resource
    private AiChatService aiChatService;
    @Resource
    private ProductMapper productMapper;

    // 全量生成的运行标记，保证同一时间只有一个全量任务在跑
    private final AtomicBoolean generateRunning = new AtomicBoolean(false);
    // 最近一次全量生成的进度。后台线程写、请求线程读，所以用 volatile 保证可见性
    private volatile EmbeddingGenerateProgress progress = new EmbeddingGenerateProgress();

    /**
     * 构造真正用于向量化的查询文本。
     * 指定了商品时，把商品名拼在问题前面，让查询和切片处在同一个语境下。
     * 用户已经把商品名写进问题里的，不重复拼接。
     */
    private String buildSearchText(EmbeddingSearchRequest request) {
        String queryText = request.getQueryText().trim();
        if (ObjectUtil.isEmpty(request.getProductId())) {
            return queryText;
        }
        Product product = productMapper.selectById(request.getProductId());
        if (ObjectUtil.isNull(product) || ObjectUtil.isEmpty(product.getName())) {
            return queryText;
        }
        if (queryText.contains(product.getName())) {
            return queryText;
        }
        return product.getName() + " " + queryText;
    }

    public int generateByChunkId(Integer chunkId) {
        if (ObjectUtil.isEmpty(chunkId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        ProductKnowledgeChunk condition = new ProductKnowledgeChunk();
        condition.setId(chunkId);
        List<ProductKnowledgeChunk> chunks = productKnowledgeChunkMapper.selectAll(condition);
        if (chunks.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        // 有启用的向量模型就用真实向量模型，否则回退本地哈希（一次生成解析一次配置）
        AiModelConfig embConfig = aiChatService.findEnabledConfig("EMBEDDING");
        productKnowledgeEmbeddingMapper.deleteByChunkId(chunkId);
        saveEmbedding(chunks.get(0), embConfig);
        return 1;
    }

    /**
     * 启动全量生成向量的后台任务，立刻返回要处理的切片总数。
     * 不在请求线程里跑完整个循环：切片数量不确定，耗时也就不确定，
     * 任何固定的 HTTP 超时时间都可能不够，所以改成后台执行 + 前端轮询进度。
     */
    public int startGenerateAll() {
        // 同一时间只允许一个全量任务，避免两次任务互相覆盖对方写入的向量
        if (!generateRunning.compareAndSet(false, true)) {
            throw new CustomException("5006", "已有全量生成任务正在执行，请等它结束后再试");
        }

        ProductKnowledgeChunk condition = new ProductKnowledgeChunk();
        condition.setChunkStatus("READY");
        List<ProductKnowledgeChunk> chunks = productKnowledgeChunkMapper.selectAll(condition);

        // 先把进度初始化好再开线程，保证前端拿到的第一次进度就是有效的
        EmbeddingGenerateProgress current = new EmbeddingGenerateProgress();
        current.setRunning(true);
        current.setTotal(chunks.size());
        current.setStartTime(DateUtil.now());
        this.progress = current;

        if (chunks.isEmpty()) {
            finishProgress(current, "没有可用切片，请先在【商品知识切片】生成切片");
            return 0;
        }

        // 整批生成前解析一次向量模型配置，避免每个切片都查一遍数据库
        AiModelConfig embConfig = aiChatService.findEnabledConfig("EMBEDDING");
        CompletableFuture.runAsync(() -> runGenerateAll(chunks, embConfig, current));
        return chunks.size();
    }

    /**
     * 后台线程真正执行的循环，每处理完一条就更新一次进度。
     */
    private void runGenerateAll(List<ProductKnowledgeChunk> chunks,
                                AiModelConfig embConfig,
                                EmbeddingGenerateProgress current) {
        try {
            for (ProductKnowledgeChunk chunk : chunks) {
                productKnowledgeEmbeddingMapper.deleteByChunkId(chunk.getId());
                try {
                    saveEmbedding(chunk, embConfig);
                    current.setSuccessCount(current.getSuccessCount() + 1);
                } catch (Exception e) {
                    // 欠费、Key 无效这类错误对每个切片都会同样失败，
                    // 继续跑只是白发几十次注定失败的请求，所以直接停下来并告诉使用者原因。
                    if (aiChatService.isFatalModelError(e)) {
                        current.setFailCount(current.getFailCount() + 1);
                        current.setProcessed(current.getProcessed() + 1);
                        finishProgress(current, "向量生成已中断：" + aiChatService.describeModelError(e)
                                + "（已成功 " + current.getSuccessCount() + " 条）");
                        return;
                    }
                    // 限流、网络抖动这类偶发失败不中断整批，
                    // 已经生成的向量保留，失败的切片下次重新生成即可。
                    current.setFailCount(current.getFailCount() + 1);
                }
                current.setProcessed(current.getProcessed() + 1);
            }
            String message = "已生成 " + current.getSuccessCount() + " 条向量";
            if (current.getFailCount() > 0) {
                message += "，" + current.getFailCount() + " 条失败（可在列表里找到未生成向量的切片重新生成）";
            }
            finishProgress(current, message);
        } catch (Exception e) {
            // 兜底：循环本身出错也要把状态置为结束，否则前端会一直轮询下去
            finishProgress(current, "生成中断：" + StrUtil.maxLength(e.getMessage(), 200));
        }
    }

    /**
     * 结束任务：写结果、置结束时间，并释放运行标记。
     */
    private void finishProgress(EmbeddingGenerateProgress current, String message) {
        current.setMessage(message);
        current.setEndTime(DateUtil.now());
        current.setRunning(false);
        generateRunning.set(false);
    }

    /**
     * 供前端轮询的进度查询。
     */
    public EmbeddingGenerateProgress getGenerateProgress() {
        return progress;
    }

    public List<ProductKnowledgeEmbedding> search(EmbeddingSearchRequest request) {
        if (ObjectUtil.isEmpty(request.getQueryText())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        if (topK < 1) {
            topK = 1;
        }
        if (topK > 20) {
            topK = 20;
        }
        // 检索前先补全查询文本：切片内容都以商品名开头，查询里没有商品名时，
        // 商品名那部分在两个向量里对不上，会把整体相似度压低并打乱排序。
        // 这里按 productId 把商品名补进查询，等价于用户自己输入了"商品名 + 问题"。
        String queryText = buildSearchText(request);

        // 查询文本用和入库时相同的方式向量化：有启用向量模型就走真实模型，否则本地哈希
        AiModelConfig embConfig = aiChatService.findEnabledConfig("EMBEDDING");
        double[] queryVector = embConfig != null
                ? aiChatService.embedWithConfig(embConfig, queryText)
                : localHashEmbed(queryText);
        ProductKnowledgeEmbedding condition = new ProductKnowledgeEmbedding();
        condition.setEmbeddingStatus("READY");
        condition.setProductId(request.getProductId());
        List<ProductKnowledgeEmbedding> embeddings = productKnowledgeEmbeddingMapper.selectAll(condition);
        for (ProductKnowledgeEmbedding embedding : embeddings) {
            double score = cosine(queryVector, parseVector(embedding.getVectorText()));
            embedding.setSimilarityScore(Math.round(score * 10000.0) / 10000.0);
        }
        return embeddings.stream()
                .sorted(Comparator.comparing(ProductKnowledgeEmbedding::getSimilarityScore).reversed())
                .limit(topK)
                .toList();
    }

    public void deleteById(Integer id) {
        productKnowledgeEmbeddingMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        productKnowledgeEmbeddingMapper.deleteBatch(ids);
    }

    public PageInfo<ProductKnowledgeEmbedding> selectPage(ProductKnowledgeEmbedding productKnowledgeEmbedding, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductKnowledgeEmbedding> list = productKnowledgeEmbeddingMapper.selectAll(productKnowledgeEmbedding);
        return PageInfo.of(list);
    }

    private void saveEmbedding(ProductKnowledgeChunk chunk, AiModelConfig embConfig) {
        // 有启用的向量模型就用真实模型向量化，并记录真实模型名和维度；否则回退本地哈希
        double[] vector;
        String modelName;
        if (embConfig != null) {
            vector = aiChatService.embedWithConfig(embConfig, chunk.getChunkContent());
            modelName = embConfig.getModelName();
        } else {
            vector = localHashEmbed(chunk.getChunkContent());
            modelName = LOCAL_EMBEDDING_MODEL;
        }
        String now = DateUtil.now();
        ProductKnowledgeEmbedding embedding = new ProductKnowledgeEmbedding();
        embedding.setChunkId(chunk.getId());
        embedding.setKnowledgeId(chunk.getKnowledgeId());
        embedding.setProductId(chunk.getProductId());
        embedding.setEmbeddingModel(modelName);
        embedding.setVectorDimension(vector.length);
        embedding.setVectorText(vectorToText(vector));
        embedding.setEmbeddingStatus("READY");
        embedding.setCreateTime(now);
        embedding.setUpdateTime(now);
        productKnowledgeEmbeddingMapper.insert(embedding);
    }

    /**
     * 本地哈希向量：未配置向量模型时的回退方案，把文本按 2-gram 散列到固定 64 维并归一化。
     * 检索质量有限，仅用于无 Key 时也能演示 RAG 流程。
     */
    private double[] localHashEmbed(String text) {
        double[] vector = new double[LOCAL_VECTOR_DIMENSION];
        String content = text == null ? "" : text.trim().toLowerCase();
        if (content.isEmpty()) {
            return vector;
        }
        for (int i = 0; i < content.length(); i++) {
            String gram = content.substring(i, Math.min(i + 2, content.length()));
            int index = Math.abs(gram.hashCode()) % LOCAL_VECTOR_DIMENSION;
            vector[index] += 1.0;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return;
        }
        double length = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
    }

    private double cosine(double[] left, double[] right) {
        // 计算真实余弦相似度 dot/(|a||b|)，兼容真实向量模型返回的未归一化向量
        int n = Math.min(left.length, right.length);
        double dot = 0;
        for (int i = 0; i < n; i++) {
            dot += left[i] * right[i];
        }
        double leftNorm = 0;
        for (double value : left) {
            leftNorm += value * value;
        }
        double rightNorm = 0;
        for (double value : right) {
            rightNorm += value * value;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String vectorToText(double[] vector) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(String.format(Locale.US, "%.6f", vector[i]));
        }
        return builder.toString();
    }

    private double[] parseVector(String vectorText) {
        // 按实际存储的维度解析，兼容本地哈希(64维)和真实向量模型(如1024维)
        if (ObjectUtil.isEmpty(vectorText)) {
            return new double[0];
        }
        String[] values = vectorText.split(",");
        double[] vector = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            vector[i] = Double.parseDouble(values[i]);
        }
        return vector;
    }
}
