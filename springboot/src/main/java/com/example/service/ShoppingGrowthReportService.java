package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.GrowthReportRequest;
import com.example.entity.ShopOrder;
import com.example.entity.ShoppingGrowthReport;
import com.example.entity.ShoppingGuideTask;
import com.example.entity.ShoppingQa;
import com.example.entity.ShoppingRecommendation;
import com.example.entity.ShoppingReviewAnalysis;
import com.example.mapper.ShoppingGrowthReportMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingGrowthReportService {

    @Resource
    private ShoppingGrowthReportMapper shoppingGrowthReportMapper;
    @Resource
    private ShopOrderService shopOrderService;
    @Resource
    private ShoppingGuideTaskService shoppingGuideTaskService;
    @Resource
    private ShoppingRecommendationService shoppingRecommendationService;
    @Resource
    private ShoppingQaService shoppingQaService;
    @Resource
    private ShoppingReviewAnalysisService shoppingReviewAnalysisService;
    @Resource
    private AiChatService aiChatService;

    public ShoppingGrowthReport generate(GrowthReportRequest request) {
        String now = DateUtil.now();
        List<ShopOrder> orders = shopOrderService.selectAll(new ShopOrder());
        List<ShoppingGuideTask> tasks = shoppingGuideTaskService.selectAll(new ShoppingGuideTask());
        List<ShoppingRecommendation> recommendations = shoppingRecommendationService.selectAll(new ShoppingRecommendation());
        List<ShoppingQa> qaList = shoppingQaService.selectAll(new ShoppingQa());
        List<ShoppingReviewAnalysis> reviewAnalyses = shoppingReviewAnalysisService.selectAll(new ShoppingReviewAnalysis());

        int orderCount = countValidOrders(orders);
        BigDecimal salesAmount = sumSalesAmount(orders);
        int guideTaskCount = tasks.size();
        int guideDoneCount = countGuideDone(tasks);
        int recommendationCount = recommendations.size();
        int qaCount = qaList.size();
        int reviewAnalysisCount = reviewAnalyses.size();
        BigDecimal conversionRate = calculateConversionRate(orderCount, guideTaskCount);

        ShoppingGrowthReport report = new ShoppingGrowthReport();
        report.setReportNo("GR" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        report.setReportTitle(resolveTitle(request));
        report.setReportType(resolveType(request));
        report.setOrderCount(orderCount);
        report.setSalesAmount(salesAmount);
        report.setGuideTaskCount(guideTaskCount);
        report.setGuideDoneCount(guideDoneCount);
        report.setRecommendationCount(recommendationCount);
        report.setQaCount(qaCount);
        report.setReviewAnalysisCount(reviewAnalysisCount);
        report.setConversionRate(conversionRate);
        report.setTopProductSummary(buildTopProductSummary(recommendations));
        report.setQaSummary(buildQaSummary(qaList));
        report.setReviewSummary(buildReviewSummary(reviewAnalyses));
        // 各项指标和汇总由代码精确统计；数据快照先生成，作为大模型撰写增长建议的输入依据
        report.setDataSnapshot(buildDataSnapshot(orderCount, salesAmount, guideTaskCount, guideDoneCount, recommendationCount, qaCount, reviewAnalysisCount, conversionRate));
        report.setGrowthSuggestion(buildGrowthSuggestion(report));
        report.setStatus("DONE");
        report.setCreateTime(now);
        report.setUpdateTime(now);
        shoppingGrowthReportMapper.insert(report);
        return report;
    }

    public void deleteById(Integer id) {
        shoppingGrowthReportMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingGrowthReportMapper.deleteById(id);
        }
    }

    public PageInfo<ShoppingGrowthReport> selectPage(ShoppingGrowthReport shoppingGrowthReport, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShoppingGrowthReport> list = shoppingGrowthReportMapper.selectAll(shoppingGrowthReport);
        return PageInfo.of(list);
    }

    private String resolveTitle(GrowthReportRequest request) {
        if (ObjectUtil.isNotNull(request) && ObjectUtil.isNotEmpty(request.getReportTitle())) {
            return request.getReportTitle();
        }
        return "AI商城运营增长报告";
    }

    private String resolveType(GrowthReportRequest request) {
        if (ObjectUtil.isNotNull(request) && ObjectUtil.isNotEmpty(request.getReportType())) {
            return request.getReportType();
        }
        return "OVERALL";
    }

    private int countValidOrders(List<ShopOrder> orders) {
        int count = 0;
        for (ShopOrder order : orders) {
            if (!"CANCELLED".equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal sumSalesAmount(List<ShopOrder> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (ShopOrder order : orders) {
            if (!"CANCELLED".equals(order.getStatus()) && order.getTotalAmount() != null) {
                total = total.add(order.getTotalAmount());
            }
        }
        return total;
    }

    private int countGuideDone(List<ShoppingGuideTask> tasks) {
        int count = 0;
        for (ShoppingGuideTask task : tasks) {
            if ("DONE".equals(task.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal calculateConversionRate(int orderCount, int guideTaskCount) {
        if (guideTaskCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(orderCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(guideTaskCount), 2, RoundingMode.HALF_UP);
    }

    private String buildTopProductSummary(List<ShoppingRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return "暂无推荐商品数据。";
        }
        return recommendations.stream()
                .filter(item -> ObjectUtil.isNotEmpty(item.getProductName()))
                .sorted(Comparator.comparing(item -> item.getRecommendScore() == null ? 0 : item.getRecommendScore(), Comparator.reverseOrder()))
                .limit(5)
                .map(item -> item.getProductName() + "，推荐分 " + nullToZero(item.getRecommendScore()) + "，状态 " + item.getStatus())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("暂无推荐商品数据。");
    }

    private String buildQaSummary(List<ShoppingQa> qaList) {
        if (qaList.isEmpty()) {
            return "暂无商品问答数据。";
        }
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (ShoppingQa qa : qaList) {
            String type = ObjectUtil.isEmpty(qa.getQuestionType()) ? "UNKNOWN" : qa.getQuestionType();
            typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            builder.append(entry.getKey()).append("：").append(entry.getValue()).append(" 次\n");
        }
        return builder.toString();
    }

    private String buildReviewSummary(List<ShoppingReviewAnalysis> analyses) {
        if (analyses.isEmpty()) {
            return "暂无评价分析数据。";
        }
        int positive = 0;
        int neutral = 0;
        int negative = 0;
        StringBuilder productSummary = new StringBuilder();
        for (ShoppingReviewAnalysis analysis : analyses) {
            if ("POSITIVE".equals(analysis.getSentimentLevel())) {
                positive++;
            } else if ("NEGATIVE".equals(analysis.getSentimentLevel())) {
                negative++;
            } else {
                neutral++;
            }
            productSummary.append(analysis.getProductName())
                    .append("：").append(analysis.getSentimentLevel())
                    .append("，好评率 ").append(analysis.getPositiveRate()).append("%\n");
        }
        return "正向商品 " + positive + " 个，中性商品 " + neutral + " 个，负向商品 " + negative + " 个。\n" + productSummary;
    }

    /**
     * 调用大模型，基于本次报告的真实统计数据和汇总，生成运营增长建议。
     * 指标本身仍由代码精确统计，这里只让模型对已有数据做解读和给建议，不编造新数据。
     */
    private String buildGrowthSuggestion(ShoppingGrowthReport report) {
        String systemPrompt = "你是电商运营增长顾问。请只根据提供的真实经营数据和汇总，给出具体、可执行的运营增长建议，"
                + "用简洁的中文分条列出；不要编造数据中没有的信息。";
        String userPrompt = "报告类型：" + report.getReportType() + "\n"
                + "核心指标：\n" + report.getDataSnapshot() + "\n\n"
                + "热门推荐商品：\n" + report.getTopProductSummary() + "\n"
                + "用户问答概况：\n" + report.getQaSummary() + "\n"
                + "评价分析概况：\n" + report.getReviewSummary() + "\n\n"
                + "请结合以上数据，给出 3-5 条运营增长建议。";
        return aiChatService.chat(systemPrompt, userPrompt);
    }

    private String buildDataSnapshot(int orderCount,
                                     BigDecimal salesAmount,
                                     int guideTaskCount,
                                     int guideDoneCount,
                                     int recommendationCount,
                                     int qaCount,
                                     int reviewAnalysisCount,
                                     BigDecimal conversionRate) {
        return "订单数：" + orderCount
                + "\n销售额：" + salesAmount
                + "\n导购任务数：" + guideTaskCount
                + "\n导购完成数：" + guideDoneCount
                + "\n推荐记录数：" + recommendationCount
                + "\n问答记录数：" + qaCount
                + "\n评价分析数：" + reviewAnalysisCount
                + "\n转化率：" + conversionRate + "%";
    }

    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
