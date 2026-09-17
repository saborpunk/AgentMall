package com.example.entity;

import java.math.BigDecimal;

public class ShoppingGrowthReport {

    private Integer id;
    private String reportNo;
    private String reportTitle;
    private String reportType;
    private Integer orderCount;
    private BigDecimal salesAmount;
    private Integer guideTaskCount;
    private Integer guideDoneCount;
    private Integer recommendationCount;
    private Integer qaCount;
    private Integer reviewAnalysisCount;
    private BigDecimal conversionRate;
    private String topProductSummary;
    private String qaSummary;
    private String reviewSummary;
    private String growthSuggestion;
    private String dataSnapshot;
    private String status;
    private String createTime;
    private String updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getGuideTaskCount() {
        return guideTaskCount;
    }

    public void setGuideTaskCount(Integer guideTaskCount) {
        this.guideTaskCount = guideTaskCount;
    }

    public Integer getGuideDoneCount() {
        return guideDoneCount;
    }

    public void setGuideDoneCount(Integer guideDoneCount) {
        this.guideDoneCount = guideDoneCount;
    }

    public Integer getRecommendationCount() {
        return recommendationCount;
    }

    public void setRecommendationCount(Integer recommendationCount) {
        this.recommendationCount = recommendationCount;
    }

    public Integer getQaCount() {
        return qaCount;
    }

    public void setQaCount(Integer qaCount) {
        this.qaCount = qaCount;
    }

    public Integer getReviewAnalysisCount() {
        return reviewAnalysisCount;
    }

    public void setReviewAnalysisCount(Integer reviewAnalysisCount) {
        this.reviewAnalysisCount = reviewAnalysisCount;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getTopProductSummary() {
        return topProductSummary;
    }

    public void setTopProductSummary(String topProductSummary) {
        this.topProductSummary = topProductSummary;
    }

    public String getQaSummary() {
        return qaSummary;
    }

    public void setQaSummary(String qaSummary) {
        this.qaSummary = qaSummary;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public String getGrowthSuggestion() {
        return growthSuggestion;
    }

    public void setGrowthSuggestion(String growthSuggestion) {
        this.growthSuggestion = growthSuggestion;
    }

    public String getDataSnapshot() {
        return dataSnapshot;
    }

    public void setDataSnapshot(String dataSnapshot) {
        this.dataSnapshot = dataSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
