package com.example.entity;

/**
 * 全量生成向量的进度。
 * 全量生成要逐个切片调用向量模型接口，耗时由切片数量决定，没法用一个固定的超时时间兜住，
 * 所以接口改成"立刻返回、后台执行"，前端靠这个对象轮询进度。
 */
public class EmbeddingGenerateProgress {

    // 是否正在生成，前端据此决定要不要继续轮询
    private boolean running;
    // 本次要处理的切片总数
    private int total;
    // 已处理数量（成功 + 失败）
    private int processed;
    // 成功生成的向量数量
    private int successCount;
    // 失败的切片数量
    private int failCount;
    // 结束后的结果说明，生成中为空
    private String message;
    // 本次任务开始时间
    private String startTime;
    // 本次任务结束时间，生成中为空
    private String endTime;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
