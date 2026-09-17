package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Product;
import com.example.entity.ProductReview;
import com.example.entity.ReviewAnalysisRequest;
import com.example.entity.ShoppingReviewAnalysis;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.example.mapper.ProductReviewMapper;
import com.example.mapper.ShoppingReviewAnalysisMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ShoppingReviewAnalysisService {

    @Resource
    private ShoppingReviewAnalysisMapper shoppingReviewAnalysisMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private ProductReviewMapper productReviewMapper;
    @Resource
    private AiChatService aiChatService;

    public ShoppingReviewAnalysis generate(ReviewAnalysisRequest request) {
        if (ObjectUtil.isNull(request) || ObjectUtil.isEmpty(request.getProductId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Product product = productMapper.selectById(request.getProductId());
        if (ObjectUtil.isNull(product)) {
            throw new CustomException("5001", "商品不存在或已被删除");
        }

        ProductReview condition = new ProductReview();
        condition.setProductId(product.getId());
        condition.setAuditStatus("APPROVED");
        List<ProductReview> reviews = productReviewMapper.selectAll(condition);
        if (reviews.isEmpty()) {
            // 没有已审核评价时给出明确原因，而不是笼统的参数异常
            throw new CustomException("5002", "商品“" + product.getName() + "”暂无已审核通过的评价，无法生成评价分析");
        }

        ShoppingReviewAnalysis analysis = buildAnalysis(product, reviews);
        shoppingReviewAnalysisMapper.deleteByProductId(product.getId());
        shoppingReviewAnalysisMapper.insert(analysis);
        return analysis;
    }

    public void deleteById(Integer id) {
        shoppingReviewAnalysisMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingReviewAnalysisMapper.deleteById(id);
        }
    }

    public List<ShoppingReviewAnalysis> selectAll(ShoppingReviewAnalysis shoppingReviewAnalysis) {
        return shoppingReviewAnalysisMapper.selectAll(shoppingReviewAnalysis);
    }

    public PageInfo<ShoppingReviewAnalysis> selectPage(ShoppingReviewAnalysis shoppingReviewAnalysis, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShoppingReviewAnalysis> list = shoppingReviewAnalysisMapper.selectAll(shoppingReviewAnalysis);
        return PageInfo.of(list);
    }

    private ShoppingReviewAnalysis buildAnalysis(Product product, List<ProductReview> reviews) {
        int positiveCount = 0;
        int neutralCount = 0;
        int negativeCount = 0;
        int totalRating = 0;
        StringBuilder sampleBuilder = new StringBuilder();

        for (ProductReview review : reviews) {
            int rating = review.getRating() == null ? 0 : review.getRating();
            totalRating += rating;
            // 按评分划分好评(>=4)、中评(=3)、差评(<3)，用于统计口径
            if (rating >= 4) {
                positiveCount++;
            } else if (rating == 3) {
                neutralCount++;
            } else {
                negativeCount++;
            }
            sampleBuilder.append("评分：").append(rating).append("，内容：").append(review.getContent()).append("\n");
        }

        int reviewCount = reviews.size();
        BigDecimal averageRating = BigDecimal.valueOf(totalRating)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
        BigDecimal positiveRate = BigDecimal.valueOf(positiveCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);

        String now = DateUtil.now();
        ShoppingReviewAnalysis analysis = new ShoppingReviewAnalysis();
        analysis.setAnalysisNo("RA" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        analysis.setProductId(product.getId());
        analysis.setProductName(product.getName());
        analysis.setReviewCount(reviewCount);
        analysis.setAverageRating(averageRating);
        analysis.setPositiveCount(positiveCount);
        analysis.setNeutralCount(neutralCount);
        analysis.setNegativeCount(negativeCount);
        analysis.setPositiveRate(positiveRate);
        analysis.setSentimentLevel(resolveSentimentLevel(averageRating, positiveRate));
        analysis.setSampleReviews(sampleBuilder.toString());
        // 数量、均分、好评率、情绪等统计口径由上面代码精确计算；
        // 优点、问题、关键词、优化建议这四项洞察交给大模型基于真实评价文本生成
        fillAiInsights(analysis, reviews);
        analysis.setStatus("DONE");
        analysis.setCreateTime(now);
        analysis.setUpdateTime(now);
        return analysis;
    }

    /**
     * 调用大模型，基于真实评价文本和统计结果生成四项评价洞察，回填到分析对象。
     */
    private void fillAiInsights(ShoppingReviewAnalysis analysis, List<ProductReview> reviews) {
        // 把每条评价的评分和内容拼给模型，作为分析依据
        StringBuilder reviewText = new StringBuilder();
        for (ProductReview review : reviews) {
            int rating = review.getRating() == null ? 0 : review.getRating();
            reviewText.append("评分").append(rating).append("：").append(review.getContent()).append("\n");
        }
        String systemPrompt = "你是电商运营分析师。请根据商品的真实用户评价，输出结构化的评价洞察。"
                + "只输出一个JSON对象，不要任何解释文字或代码围栏。";
        String userPrompt = "商品名称：" + analysis.getProductName() + "\n"
                + "评价总数：" + analysis.getReviewCount()
                + "，平均分：" + analysis.getAverageRating()
                + "，好评数：" + analysis.getPositiveCount()
                + "，中评数：" + analysis.getNeutralCount()
                + "，差评数：" + analysis.getNegativeCount()
                + "，好评率：" + analysis.getPositiveRate() + "%\n\n"
                + "全部评价如下：\n" + reviewText + "\n"
                + "请输出JSON，包含四个中文字符串字段：\n"
                + "advantageSummary：好评中反映的商品优点总结；\n"
                + "problemSummary：差评中反映的主要问题总结；\n"
                + "keywordSummary：高频关键词，用中文顿号分隔；\n"
                + "improvementSuggestion：给商家的优化建议。";
        JSONObject json = aiChatService.chatForJson(systemPrompt, userPrompt);
        analysis.setAdvantageSummary(json.getStr("advantageSummary", ""));
        analysis.setProblemSummary(json.getStr("problemSummary", ""));
        analysis.setKeywordSummary(json.getStr("keywordSummary", ""));
        analysis.setImprovementSuggestion(json.getStr("improvementSuggestion", ""));
    }

    private String resolveSentimentLevel(BigDecimal averageRating, BigDecimal positiveRate) {
        if (averageRating.compareTo(BigDecimal.valueOf(4.5)) >= 0 && positiveRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "POSITIVE";
        }
        if (averageRating.compareTo(BigDecimal.valueOf(3.5)) >= 0) {
            return "NEUTRAL";
        }
        return "NEGATIVE";
    }

}
