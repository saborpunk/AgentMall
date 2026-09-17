package com.example.entity;

import java.math.BigDecimal;

public class ShoppingGuideTask {

    private Integer id;
    private String taskNo;
    private Integer userId;
    private String userName;
    private String demandText;
    private BigDecimal budgetAmount;
    private Integer productId;
    private String productName;
    private String status;
    private String matchedProductIds;
    private String recommendationResult;
    private String executeMessage;
    private String createTime;
    private String updateTime;
    private String executeTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDemandText() {
        return demandText;
    }

    public void setDemandText(String demandText) {
        this.demandText = demandText;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMatchedProductIds() {
        return matchedProductIds;
    }

    public void setMatchedProductIds(String matchedProductIds) {
        this.matchedProductIds = matchedProductIds;
    }

    public String getRecommendationResult() {
        return recommendationResult;
    }

    public void setRecommendationResult(String recommendationResult) {
        this.recommendationResult = recommendationResult;
    }

    public String getExecuteMessage() {
        return executeMessage;
    }

    public void setExecuteMessage(String executeMessage) {
        this.executeMessage = executeMessage;
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

    public String getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(String executeTime) {
        this.executeTime = executeTime;
    }
}
