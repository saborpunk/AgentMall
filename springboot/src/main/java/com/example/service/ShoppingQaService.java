package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AfterSaleRule;
import com.example.entity.BusinessToolRequest;
import com.example.entity.BusinessToolResult;
import com.example.entity.EmbeddingSearchRequest;
import com.example.entity.Product;
import com.example.entity.ProductKnowledgeEmbedding;
import com.example.entity.ProductToolRequest;
import com.example.entity.ProductToolResult;
import com.example.entity.ShoppingQa;
import com.example.entity.ShoppingQaRequest;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.example.mapper.ShoppingQaMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingQaService {

    // 召回切片的最低余弦相似度。低于这个分数视为没检索到相关资料，
    // 宁可回答"资料未覆盖"，也不拿不相关的切片让模型硬答。
    private static final double MIN_SIMILARITY_SCORE = 0.5;

    private static final String TYPE_PRODUCT = "PRODUCT";
    private static final String TYPE_PRICE_STOCK = "PRICE_STOCK";
    private static final String TYPE_ORDER = "ORDER";
    private static final String TYPE_AFTER_SALE = "AFTER_SALE";

    @Resource
    private ShoppingQaMapper shoppingQaMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private ProductToolService productToolService;
    @Resource
    private BusinessToolService businessToolService;
    @Resource
    private AfterSaleRuleService afterSaleRuleService;
    @Resource
    private ProductKnowledgeEmbeddingService productKnowledgeEmbeddingService;
    @Resource
    private AiChatService aiChatService;

    public ShoppingQa ask(ShoppingQaRequest request) {
        validate(request);
        String now = DateUtil.now();
        ShoppingQa qa = new ShoppingQa();
        qa.setQaNo("QA" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        qa.setUserId(request.getUserId());
        qa.setQuestionType(request.getQuestionType());
        qa.setQuestionText(request.getQuestionText());
        qa.setProductId(request.getProductId());
        qa.setProductName(request.getProductName());
        qa.setOrderId(request.getOrderId());
        qa.setOrderNo(request.getOrderNo());
        qa.setStatus("DONE");
        qa.setCreateTime(now);
        qa.setUpdateTime(now);

        if (TYPE_PRODUCT.equals(request.getQuestionType())) {
            answerProductQuestion(request, qa);
        } else if (TYPE_PRICE_STOCK.equals(request.getQuestionType())) {
            answerPriceStockQuestion(request, qa);
        } else if (TYPE_ORDER.equals(request.getQuestionType())) {
            answerOrderQuestion(request, qa);
        } else if (TYPE_AFTER_SALE.equals(request.getQuestionType())) {
            answerAfterSaleQuestion(request, qa);
        }

        shoppingQaMapper.insert(qa);
        return qa;
    }

    public void deleteById(Integer id) {
        shoppingQaMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingQaMapper.deleteById(id);
        }
    }

    public List<ShoppingQa> selectAll(ShoppingQa shoppingQa) {
        return shoppingQaMapper.selectAll(shoppingQa);
    }

    public PageInfo<ShoppingQa> selectPage(ShoppingQa shoppingQa, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShoppingQa> list = shoppingQaMapper.selectAll(shoppingQa);
        return PageInfo.of(list);
    }

    private void validate(ShoppingQaRequest request) {
        if (ObjectUtil.isNull(request)
                || ObjectUtil.isEmpty(request.getQuestionType())
                || ObjectUtil.isEmpty(request.getQuestionText())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (!TYPE_PRODUCT.equals(request.getQuestionType())
                && !TYPE_PRICE_STOCK.equals(request.getQuestionType())
                && !TYPE_ORDER.equals(request.getQuestionType())
                && !TYPE_AFTER_SALE.equals(request.getQuestionType())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (TYPE_ORDER.equals(request.getQuestionType())
                && ObjectUtil.isEmpty(request.getOrderId())
                && ObjectUtil.isEmpty(request.getOrderNo())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if ((TYPE_PRICE_STOCK.equals(request.getQuestionType()) || TYPE_PRODUCT.equals(request.getQuestionType()))
                && ObjectUtil.isEmpty(request.getProductId())
                && ObjectUtil.isEmpty(request.getProductName())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }

    private void answerProductQuestion(ShoppingQaRequest request, ShoppingQa qa) {
        Product product = resolveProduct(request);
        qa.setProductId(product.getId());
        qa.setProductName(product.getName());

        // 第一步（检索 Retrieval）：用向量相似度从商品知识库召回与问题最相关的资料切片
        EmbeddingSearchRequest searchRequest = new EmbeddingSearchRequest();
        searchRequest.setProductId(product.getId());
        searchRequest.setQueryText(request.getQuestionText());
        searchRequest.setTopK(3);
        List<ProductKnowledgeEmbedding> embeddings = productKnowledgeEmbeddingService.search(searchRequest);

        // 检索永远会返回 topK 条，哪怕全都不相关。这里按相似度阈值筛一遍，
        // 把明显不相关的切片挡掉，避免拿着无关资料让模型硬答。
        embeddings = embeddings.stream()
                .filter(item -> item.getSimilarityScore() != null
                        && item.getSimilarityScore() >= MIN_SIMILARITY_SCORE)
                .toList();

        if (embeddings.isEmpty()) {
            // 没召回到资料就如实说明，不让模型脱离资料编造答案
            qa.setAnswerText("没有找到和这个问题相关的商品资料，无法基于资料作答。"
                    + "如果商品还没维护过资料，可以先在【商品知识库】导入或新增资料，"
                    + "再依次生成切片和向量；如果资料已经有了，说明这个问题超出了现有资料的范围。");
            qa.setEvidenceContent("");
            qa.setToolTrace("PRODUCT_KNOWLEDGE_SEARCH（无相关切片，未调用大模型）");
            return;
        }

        // 把召回的切片拼成上下文，作为大模型作答的依据（RAG 的“增强 Augmented”部分）
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < embeddings.size(); i++) {
            ProductKnowledgeEmbedding embedding = embeddings.get(i);
            context.append("资料").append(i + 1).append("：").append(embedding.getChunkTitle()).append("\n");
            context.append(embedding.getChunkContent()).append("\n\n");
        }

        // 第二步（生成 Generation）：真实调用大模型，要求它只依据召回资料回答商品问题
        String systemPrompt = "你是电商平台的商品导购助手。请只依据提供的商品资料回答用户问题，"
                + "用简洁、专业、口语化的中文作答；如果资料中没有相关信息，直接说明资料未覆盖，不要编造。";
        String userPrompt = "商品名称：" + product.getName() + "\n\n"
                + "商品资料如下：\n" + context
                + "用户问题：" + request.getQuestionText() + "\n\n"
                + "请依据上述资料回答：";
        String answer = aiChatService.chat(systemPrompt, userPrompt);

        qa.setAnswerText(answer);
        // 证据仍记录本次真实召回的切片，保证回答可追溯（RAG 的可解释性）
        qa.setEvidenceContent(buildEmbeddingEvidence(embeddings));
        qa.setToolTrace("PRODUCT_KNOWLEDGE_SEARCH -> AI_CHAT（大模型基于召回资料生成）");
    }

    private void answerPriceStockQuestion(ShoppingQaRequest request, ShoppingQa qa) {
        ProductToolRequest toolRequest = new ProductToolRequest();
        toolRequest.setProductId(request.getProductId());
        toolRequest.setProductName(request.getProductName());
        toolRequest.setQuantity(1);
        ProductToolResult price = productToolService.queryProductPrice(toolRequest);
        ProductToolResult stock = productToolService.queryProductStock(toolRequest);
        ProductToolResult promotion = productToolService.queryProductPromotion(toolRequest);

        qa.setProductId(price.getProductId());
        qa.setProductName(price.getProductName());
        qa.setAnswerText("商品：" + price.getProductName()
                + "\n当前价格：" + price.getPrice()
                + "\n原价：" + price.getOriginalPrice()
                + "\n库存数量：" + stock.getStockQuantity()
                + "\n库存判断：" + stock.getMessage()
                + "\n优惠信息：" + promotion.getMessage()
                + "\n优惠金额：" + promotion.getDiscountAmount());
        qa.setEvidenceContent("价格工具返回：" + price.getMessage()
                + "\n库存工具返回：" + stock.getMessage()
                + "\n优惠工具返回：" + promotion.getMessage());
        qa.setToolTrace("PRODUCT_PRICE_QUERY -> PRODUCT_STOCK_QUERY -> PRODUCT_PROMOTION_QUERY");
    }

    private void answerOrderQuestion(ShoppingQaRequest request, ShoppingQa qa) {
        BusinessToolRequest toolRequest = new BusinessToolRequest();
        toolRequest.setOrderId(request.getOrderId());
        toolRequest.setOrderNo(request.getOrderNo());
        BusinessToolResult order = businessToolService.queryOrderStatus(toolRequest);

        qa.setOrderId(order.getOrderId());
        qa.setOrderNo(order.getOrderNo());
        qa.setAnswerText("订单：" + order.getOrderNo()
                + "\n当前状态：" + order.getOrderStatus()
                + "\n订单金额：" + order.getTotalAmount()
                + "\n商品件数：" + order.getTotalQuantity()
                + "\n收货人：" + order.getReceiverName()
                + "\n收货电话：" + order.getReceiverPhone()
                + "\n收货地址：" + order.getReceiverAddress()
                + "\n支付时间：" + nullToText(order.getPayTime())
                + "\n发货时间：" + nullToText(order.getShipTime())
                + "\n完成时间：" + nullToText(order.getFinishTime())
                + "\n取消时间：" + nullToText(order.getCancelTime()));
        qa.setEvidenceContent("订单状态工具返回：" + order.getMessage());
        qa.setToolTrace("ORDER_STATUS_QUERY -> businessToolService.queryOrderStatus");
    }

    private void answerAfterSaleQuestion(ShoppingQaRequest request, ShoppingQa qa) {
        Product product = resolveProductIfPossible(request);
        if (ObjectUtil.isNotNull(product)) {
            qa.setProductId(product.getId());
            qa.setProductName(product.getName());
        }

        AfterSaleRule condition = new AfterSaleRule();
        condition.setIsEnabled(1);
        List<AfterSaleRule> rules = afterSaleRuleService.selectAll(condition);
        String question = request.getQuestionText();
        List<AfterSaleRule> matched = rules.stream()
                .filter(rule -> matchRule(rule, product, question))
                .limit(3)
                .toList();
        if (matched.isEmpty()) {
            matched = rules.stream().limit(3).toList();
        }

        StringBuilder answer = new StringBuilder();
        answer.append("根据当前售后规则，建议参考以下处理方式：\n");
        for (int i = 0; i < matched.size(); i++) {
            AfterSaleRule rule = matched.get(i);
            answer.append(i + 1).append(". ").append(rule.getRuleName()).append("\n");
            answer.append("适用场景：").append(rule.getApplyScene()).append("\n");
            answer.append("适用条件：").append(rule.getConditionText()).append("\n");
            answer.append("处理流程：").append(rule.getProcessText()).append("\n");
            answer.append("处理时效：").append(nullToText(rule.getTimeLimit())).append("\n");
            answer.append("联系渠道：").append(nullToText(rule.getContactChannel())).append("\n");
        }
        qa.setAnswerText(answer.toString());
        qa.setEvidenceContent(buildAfterSaleEvidence(matched));
        qa.setToolTrace("AFTER_SALE_RULE_QUERY -> afterSaleRuleService.selectAll");
    }

    private Product resolveProduct(ShoppingQaRequest request) {
        Product product = resolveProductIfPossible(request);
        if (ObjectUtil.isNull(product)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return product;
    }

    private Product resolveProductIfPossible(ShoppingQaRequest request) {
        Product product = null;
        if (ObjectUtil.isNotEmpty(request.getProductId())) {
            product = productMapper.selectById(request.getProductId());
        } else if (ObjectUtil.isNotEmpty(request.getProductName())) {
            Product condition = new Product();
            condition.setName(request.getProductName());
            List<Product> products = productMapper.selectAll(condition);
            if (!products.isEmpty()) {
                product = products.get(0);
            }
        }
        return product;
    }

    private boolean matchRule(AfterSaleRule rule, Product product, String question) {
        boolean categoryMatched = product == null
                || rule.getCategoryId() == null
                || rule.getCategoryId().equals(product.getCategoryId());
        boolean keywordMatched = contains(question, rule.getRuleType())
                || contains(question, rule.getRuleName())
                || contains(question, rule.getApplyScene())
                || contains(question, rule.getConditionText());
        return categoryMatched && keywordMatched;
    }

    private boolean contains(String source, String target) {
        return ObjectUtil.isNotEmpty(source)
                && ObjectUtil.isNotEmpty(target)
                && source.toLowerCase().contains(target.toLowerCase());
    }

    private String buildEmbeddingEvidence(List<ProductKnowledgeEmbedding> embeddings) {
        StringBuilder builder = new StringBuilder();
        for (ProductKnowledgeEmbedding embedding : embeddings) {
            builder.append("切片ID：").append(embedding.getChunkId())
                    .append("，商品：").append(embedding.getProductName())
                    .append("，标题：").append(embedding.getChunkTitle())
                    .append("，相似度：").append(embedding.getSimilarityScore())
                    .append("\n");
        }
        return builder.toString();
    }

    private String buildAfterSaleEvidence(List<AfterSaleRule> rules) {
        StringBuilder builder = new StringBuilder();
        for (AfterSaleRule rule : rules) {
            builder.append("规则ID：").append(rule.getId())
                    .append("，规则名称：").append(rule.getRuleName())
                    .append("，规则类型：").append(rule.getRuleType())
                    .append("\n");
        }
        return builder.toString();
    }

    private String nullToText(String value) {
        return ObjectUtil.isEmpty(value) ? "暂无" : value;
    }
}
