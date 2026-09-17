package com.example.entity;

import java.math.BigDecimal;

public class ShoppingReviewAnalysis {

    private Integer id;
    private String analysisNo;
    private Integer productId;
    private String productName;
    private Integer reviewCount;
    private BigDecimal averageRating;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private BigDecimal positiveRate;
    private String sentimentLevel;
    private String advantageSummary;
    private String problemSummary;
    private String keywordSummary;
    private String improvementSuggestion;
    private String sampleReviews;
    private String status;
    private String createTime;
    private String updateTime;
    private String productNo;
    private String coverImage;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAnalysisNo() {
        return analysisNo;
    }

    public void setAnalysisNo(String analysisNo) {
        this.analysisNo = analysisNo;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getPositiveCount() {
        return positiveCount;
    }

    public void setPositiveCount(Integer positiveCount) {
        this.positiveCount = positiveCount;
    }

    public Integer getNeutralCount() {
        return neutralCount;
    }

    public void setNeutralCount(Integer neutralCount) {
        this.neutralCount = neutralCount;
    }

    public Integer getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(Integer negativeCount) {
        this.negativeCount = negativeCount;
    }

    public BigDecimal getPositiveRate() {
        return positiveRate;
    }

    public void setPositiveRate(BigDecimal positiveRate) {
        this.positiveRate = positiveRate;
    }

    public String getSentimentLevel() {
        return sentimentLevel;
    }

    public void setSentimentLevel(String sentimentLevel) {
        this.sentimentLevel = sentimentLevel;
    }

    public String getAdvantageSummary() {
        return advantageSummary;
    }

    public void setAdvantageSummary(String advantageSummary) {
        this.advantageSummary = advantageSummary;
    }

    public String getProblemSummary() {
        return problemSummary;
    }

    public void setProblemSummary(String problemSummary) {
        this.problemSummary = problemSummary;
    }

    public String getKeywordSummary() {
        return keywordSummary;
    }

    public void setKeywordSummary(String keywordSummary) {
        this.keywordSummary = keywordSummary;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public void setImprovementSuggestion(String improvementSuggestion) {
        this.improvementSuggestion = improvementSuggestion;
    }

    public String getSampleReviews() {
        return sampleReviews;
    }

    public void setSampleReviews(String sampleReviews) {
        this.sampleReviews = sampleReviews;
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

    public String getProductNo() {
        return productNo;
    }

    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
}
