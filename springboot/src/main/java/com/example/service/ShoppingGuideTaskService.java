package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AgentRun;
import com.example.entity.AgentStep;
import com.example.entity.BusinessToolRequest;
import com.example.entity.BusinessToolResult;
import com.example.entity.Product;
import com.example.entity.ProductToolRequest;
import com.example.entity.ProductToolResult;
import com.example.entity.ShoppingGuideTask;
import com.example.entity.ShoppingRecommendation;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.example.mapper.ShoppingGuideTaskMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShoppingGuideTaskService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingGuideTaskService.class);

    @Resource
    private ShoppingGuideTaskMapper shoppingGuideTaskMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private BusinessToolService businessToolService;
    @Resource
    private ProductToolService productToolService;
    @Resource
    private AgentRunService agentRunService;
    @Resource
    private AgentStepService agentStepService;
    @Resource
    private ShoppingRecommendationService shoppingRecommendationService;
    @Resource
    private AiChatService aiChatService;

    public void add(ShoppingGuideTask shoppingGuideTask) {
        validate(shoppingGuideTask);
        if (ObjectUtil.isEmpty(shoppingGuideTask.getTaskNo())) {
            shoppingGuideTask.setTaskNo("GT" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        }
        ShoppingGuideTask dbTask = shoppingGuideTaskMapper.selectByTaskNo(shoppingGuideTask.getTaskNo());
        if (ObjectUtil.isNotNull(dbTask)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        String now = DateUtil.now();
        shoppingGuideTask.setStatus("WAITING");
        shoppingGuideTask.setCreateTime(now);
        shoppingGuideTask.setUpdateTime(now);
        shoppingGuideTaskMapper.insert(shoppingGuideTask);
    }

    public void updateById(ShoppingGuideTask shoppingGuideTask) {
        if (ObjectUtil.isEmpty(shoppingGuideTask.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(shoppingGuideTask);
        ShoppingGuideTask dbTask = shoppingGuideTaskMapper.selectById(shoppingGuideTask.getId());
        if (ObjectUtil.isNull(dbTask)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        shoppingGuideTask.setStatus("WAITING");
        shoppingGuideTask.setMatchedProductIds("");
        shoppingGuideTask.setRecommendationResult("");
        shoppingGuideTask.setExecuteMessage("");
        shoppingGuideTask.setUpdateTime(DateUtil.now());
        shoppingGuideTaskMapper.updateById(shoppingGuideTask);
    }

    public void deleteById(Integer id) {
        shoppingGuideTaskMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingGuideTaskMapper.deleteById(id);
        }
    }

    public List<ShoppingGuideTask> selectAll(ShoppingGuideTask shoppingGuideTask) {
        return shoppingGuideTaskMapper.selectAll(shoppingGuideTask);
    }

    public PageInfo<ShoppingGuideTask> selectPage(ShoppingGuideTask shoppingGuideTask, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShoppingGuideTask> list = shoppingGuideTaskMapper.selectAll(shoppingGuideTask);
        return PageInfo.of(list);
    }

    public ShoppingGuideTask execute(Integer id) {
        ShoppingGuideTask task = shoppingGuideTaskMapper.selectById(id);
        if (ObjectUtil.isNull(task)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        AgentRun agentRun = agentRunService.startGuideRun(task);
        try {
            executeTask(task, agentRun);
            agentRunService.finishGuideRun(agentRun, task);
            return shoppingGuideTaskMapper.selectById(task.getId());
        } catch (RuntimeException e) {
            // 记录失败原因本身如果再抛异常（例如错误信息写库失败），
            // 会把真正的失败原因顶掉，页面上只能看到"记录失败"这一层的报错。
            // 所以这里单独兜住，保证原始异常一定能抛出去。
            try {
                agentRunService.failGuideRun(agentRun, buildErrorMessage(e));
            } catch (Exception recordError) {
                log.error("记录导购任务失败原因时出错，任务ID：{}", id, recordError);
            }
            log.error("导购任务执行失败，任务ID：{}", id, e);
            throw e;
        }
    }

    /**
     * 取异常的失败原因文字。
     * CustomException 没有把提示信息传给父类，getMessage() 返回的是 null，
     * 直接存库会让 agent_run.error_message 变成空值，页面上看不到失败原因，
     * 所以这里优先取它自己的 msg；其他异常没有 message 时用异常类名兜底。
     */
    private String buildErrorMessage(RuntimeException e) {
        if (e instanceof CustomException customException) {
            return customException.getMsg();
        }
        return StrUtil.isBlank(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
    }

    // Agent 单次运行最多允许的对话轮数，防止模型陷入无限工具调用
    private static final int MAX_AGENT_ITERATIONS = 10;
    // Agent 最多提交的推荐数量
    private static final int MAX_RECOMMENDATIONS = 3;

    /**
     * 真 Agent 执行：把导购工具以 Function Calling 形式交给大模型，由模型自主决定
     * 先检索候选商品、再逐个查价格/库存/优惠、读用户画像，最后调用 submit_recommendations 提交结果。
     * 每次真实的工具调用都作为一条 AgentStep 记录下来，最终用真实工具数据落地结构化推荐。
     */
    private void executeTask(ShoppingGuideTask task, AgentRun agentRun) {
        JSONArray tools = buildAgentTools();
        JSONArray messages = new JSONArray();
        messages.add(buildSystemMessage());
        messages.add(buildUserMessage(task));

        // 收集模型通过 submit_recommendations 提交的商品ID和推荐理由
        List<AgentSubmission> submissions = new ArrayList<>();
        boolean submitted = false;
        int stepOrder = 1;

        for (int round = 0; round < MAX_AGENT_ITERATIONS && !submitted; round++) {
            // 让模型基于当前对话和工具决定下一步（可能返回文本，也可能返回若干 tool_calls）
            JSONObject assistant = aiChatService.chatCompletion(messages, tools);
            // 助手消息必须原样回填到对话里（含 tool_calls），否则下一轮无法关联工具结果
            if (assistant.get("content") == null) {
                assistant.set("content", "");
            }
            messages.add(assistant);

            JSONArray toolCalls = assistant.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                // 模型没有再调用工具，视为它已给出最终判断，结束循环
                break;
            }

            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                String callId = toolCall.getStr("id");
                JSONObject function = toolCall.getJSONObject("function");
                String name = function.getStr("name");
                String argumentsRaw = function.getStr("arguments");
                JSONObject args = parseArguments(argumentsRaw);

                // 每一次工具调用都真实记录为一条 Agent 执行步骤
                AgentStep step = agentStepService.startStep(agentRun, stepOrder++, "TOOL_CALL", toolLabel(name), name, argumentsRaw);
                String toolResult;
                if ("submit_recommendations".equals(name)) {
                    // 模型提交最终推荐：捕获 items，后续用真实工具数据落地
                    submissions.addAll(parseSubmissions(args));
                    submitted = true;
                    toolResult = "已收到 " + submissions.size() + " 条推荐提交";
                } else {
                    toolResult = executeAgentTool(name, args, task);
                }
                agentStepService.finishStep(step, toolResult);

                // 把工具结果以 tool 角色回填对话，供模型下一轮参考
                JSONObject toolMessage = new JSONObject();
                toolMessage.set("role", "tool");
                toolMessage.set("tool_call_id", callId);
                toolMessage.set("name", name);
                toolMessage.set("content", toolResult);
                messages.add(toolMessage);
            }
        }

        materializeRecommendations(task, agentRun, submissions, stepOrder);
    }

    /**
     * 根据模型提交的商品ID，用真实工具重新校验价格/库存/优惠并落地结构化推荐结果。
     * 只有真实在售、可购买的商品才会进入最终推荐，保证页面数据可反查。
     */
    private void materializeRecommendations(ShoppingGuideTask task, AgentRun agentRun, List<AgentSubmission> submissions, int stepOrder) {
        List<String> recommendBlocks = new ArrayList<>();
        List<Integer> matchedIds = new ArrayList<>();
        List<ShoppingRecommendation> recommendations = new ArrayList<>();

        for (AgentSubmission submission : submissions) {
            if (recommendations.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
            Product product = productMapper.selectById(submission.productId);
            if (product == null || !"ON_SALE".equals(product.getStatus())) {
                continue;
            }
            ProductToolRequest toolRequest = new ProductToolRequest();
            toolRequest.setProductId(product.getId());
            toolRequest.setQuantity(1);
            ProductToolResult priceResult = productToolService.queryProductPrice(toolRequest);
            ProductToolResult stockResult = productToolService.queryProductStock(toolRequest);
            ProductToolResult promotionResult = productToolService.queryProductPromotion(toolRequest);
            // 库存不足的商品不进入推荐，避免推荐买不到的商品
            if (stockResult.getCanBuy() == null || stockResult.getCanBuy() != 1) {
                continue;
            }
            // 推荐理由用模型提交的理由；模型没给就用证据摘要兜底
            String reason = ObjectUtil.isEmpty(submission.reason)
                    ? buildEvidenceSummary(product, priceResult, stockResult, promotionResult)
                    : submission.reason;
            matchedIds.add(product.getId());
            recommendBlocks.add(buildRecommendBlock(product, priceResult, stockResult, promotionResult, reason));
            recommendations.add(buildRecommendation(task, agentRun, product, priceResult, stockResult, promotionResult, reason, recommendations.size() + 1));
        }

        AgentStep finalStep = agentStepService.startStep(agentRun, stepOrder, "RECOMMENDATION_GENERATE", "生成导购推荐结果", "", "模型提交商品ID：" + submissions.stream().map(s -> String.valueOf(s.productId)).collect(Collectors.joining(",")));
        if (recommendBlocks.isEmpty()) {
            task.setStatus("FAILED");
            task.setMatchedProductIds("");
            task.setRecommendationResult("");
            task.setExecuteMessage("AI 未产出可购买的推荐商品（可能是候选不足或库存不足）");
            shoppingRecommendationService.saveTaskRecommendations(task.getId(), new ArrayList<>());
            agentStepService.failStep(finalStep, task.getExecuteMessage());
        } else {
            task.setStatus("DONE");
            task.setMatchedProductIds(matchedIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            task.setRecommendationResult(String.join("\n\n", recommendBlocks));
            task.setExecuteMessage("已完成导购任务，AI 生成 " + recommendBlocks.size() + " 条推荐");
            shoppingRecommendationService.saveTaskRecommendations(task.getId(), recommendations);
            agentStepService.finishStep(finalStep, task.getRecommendationResult());
        }
        task.setExecuteTime(DateUtil.now());
        task.setUpdateTime(DateUtil.now());
        shoppingGuideTaskMapper.updateById(task);
    }

    /**
     * 构造交给大模型的系统提示词，说明 Agent 角色、可用工具的使用方式和输出约束。
     */
    private JSONObject buildSystemMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是电商平台的智能导购 Agent。你的目标是根据用户购物需求，为用户挑选最合适的商品。\n");
        sb.append("你可以调用以下工具获取真实数据：\n");
        sb.append("- search_candidate_products：按关键词和预算检索候选商品；\n");
        sb.append("- query_product_price / query_product_stock / query_product_promotion：查询某商品的真实价格、库存、优惠；\n");
        sb.append("- query_user_profile：读取当前用户画像（累计订单、余额等）；\n");
        sb.append("- submit_recommendations：提交最终推荐（最多 ").append(MAX_RECOMMENDATIONS).append(" 个商品，每个含 productId 和 reason）。\n");
        sb.append("工作要求：先检索候选商品，再对候选逐个查询价格、库存、优惠，只推荐在售且有库存、价格不超预算的商品；");
        sb.append("推荐理由必须基于工具查到的真实数据，不要编造参数或价格。信息足够后调用 submit_recommendations 结束。\n");
        JSONObject message = new JSONObject();
        message.set("role", "system");
        message.set("content", sb.toString());
        return message;
    }

    /**
     * 构造用户消息，包含本次导购任务的需求、预算、基准商品和用户ID。
     */
    private JSONObject buildUserMessage(ShoppingGuideTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("购物需求：").append(task.getDemandText()).append("\n");
        sb.append("预算金额：").append(task.getBudgetAmount() == null ? "未指定" : task.getBudgetAmount()).append("\n");
        sb.append("商品关键词：").append(ObjectUtil.isEmpty(task.getProductName()) ? "无" : task.getProductName()).append("\n");
        sb.append("基准商品ID：").append(task.getProductId() == null ? "无" : task.getProductId()).append("\n");
        sb.append("当前用户ID：").append(task.getUserId() == null ? "无（可跳过用户画像）" : task.getUserId());
        JSONObject message = new JSONObject();
        message.set("role", "user");
        message.set("content", sb.toString());
        return message;
    }

    /**
     * 构造 Function Calling 工具定义（OpenAI 兼容 tools 格式）。
     */
    private JSONArray buildAgentTools() {
        JSONArray tools = new JSONArray();
        // 检索候选商品
        JSONObject searchProps = new JSONObject();
        searchProps.set("keyword", propSchema("string", "商品关键词，可为空"));
        searchProps.set("maxPrice", propSchema("number", "价格上限，可为空"));
        tools.add(functionTool("search_candidate_products", "按关键词和预算上限检索上架商品，返回候选商品列表（含id、名称、价格、标签、卖点）", searchProps, new String[]{}));
        // 价格/库存/优惠查询（都只需 productId）
        JSONObject idProps = new JSONObject();
        idProps.set("productId", propSchema("integer", "商品ID"));
        tools.add(functionTool("query_product_price", "查询指定商品的真实价格和原价", idProps, new String[]{"productId"}));
        tools.add(functionTool("query_product_stock", "查询指定商品的库存数量和是否可购买", idProps, new String[]{"productId"}));
        tools.add(functionTool("query_product_promotion", "查询指定商品的优惠金额和折扣率", idProps, new String[]{"productId"}));
        // 用户画像（无参，用当前任务的用户）
        tools.add(functionTool("query_user_profile", "读取当前用户的画像：累计订单数、累计消费、账户余额、最近订单状态", new JSONObject(), new String[]{}));
        // 提交推荐
        JSONObject itemProps = new JSONObject();
        itemProps.set("productId", propSchema("integer", "推荐商品ID"));
        itemProps.set("reason", propSchema("string", "推荐理由，基于真实数据"));
        JSONObject itemSchema = new JSONObject();
        itemSchema.set("type", "object");
        itemSchema.set("properties", itemProps);
        itemSchema.set("required", JSONUtil.parseArray(new String[]{"productId", "reason"}));
        JSONObject itemsArray = new JSONObject();
        itemsArray.set("type", "array");
        itemsArray.set("items", itemSchema);
        itemsArray.set("description", "推荐商品列表，最多 " + MAX_RECOMMENDATIONS + " 个");
        JSONObject submitProps = new JSONObject();
        submitProps.set("items", itemsArray);
        tools.add(functionTool("submit_recommendations", "提交最终推荐结果", submitProps, new String[]{"items"}));
        return tools;
    }

    // 组装单个 function 工具定义
    private JSONObject functionTool(String name, String description, JSONObject properties, String[] required) {
        JSONObject parameters = new JSONObject();
        parameters.set("type", "object");
        parameters.set("properties", properties);
        parameters.set("required", JSONUtil.parseArray(required));
        JSONObject function = new JSONObject();
        function.set("name", name);
        function.set("description", description);
        function.set("parameters", parameters);
        JSONObject tool = new JSONObject();
        tool.set("type", "function");
        tool.set("function", function);
        return tool;
    }

    // 组装单个参数的 schema
    private JSONObject propSchema(String type, String description) {
        JSONObject schema = new JSONObject();
        schema.set("type", type);
        schema.set("description", description);
        return schema;
    }

    /**
     * 真正执行模型请求的工具，返回给模型的结果文本。
     */
    private String executeAgentTool(String name, JSONObject args, ShoppingGuideTask task) {
        switch (name) {
            case "search_candidate_products":
                return searchCandidateProducts(args.getStr("keyword"), args.getBigDecimal("maxPrice"));
            case "query_product_price": {
                ProductToolResult r = productToolService.queryProductPrice(productRequest(args.getInt("productId")));
                return "商品：" + r.getProductName() + "，现价：" + r.getPrice() + "，原价：" + r.getOriginalPrice();
            }
            case "query_product_stock": {
                ProductToolResult r = productToolService.queryProductStock(productRequest(args.getInt("productId")));
                return "商品：" + r.getProductName() + "，库存数量：" + r.getStockQuantity() + "，是否可买：" + r.getCanBuy();
            }
            case "query_product_promotion": {
                ProductToolResult r = productToolService.queryProductPromotion(productRequest(args.getInt("productId")));
                return "商品：" + r.getProductName() + "，优惠金额：" + r.getDiscountAmount() + "，折扣率：" + r.getDiscountRate();
            }
            case "query_user_profile": {
                if (ObjectUtil.isEmpty(task.getUserId())) {
                    return "当前任务未选择用户，无用户画像。";
                }
                BusinessToolResult r = queryUserProfile(task);
                if (r == null) {
                    return "未查询到用户画像。";
                }
                return "用户：" + r.getUserName() + "，累计订单数：" + r.getOrderCount()
                        + "，累计消费：" + r.getOrderAmount() + "，账户余额：" + r.getBalance()
                        + "，最近订单状态：" + r.getLatestOrderStatus();
            }
            default:
                return "未知工具：" + name;
        }
    }

    // 检索候选上架商品，返回 JSON 文本供模型解析
    private String searchCandidateProducts(String keyword, BigDecimal maxPrice) {
        Product condition = new Product();
        condition.setStatus("ON_SALE");
        if (ObjectUtil.isNotEmpty(keyword)) {
            condition.setName(keyword);
        }
        List<Product> products = productMapper.selectAll(condition);
        JSONArray array = new JSONArray();
        int count = 0;
        for (Product product : products) {
            if (!"ON_SALE".equals(product.getStatus())) {
                continue;
            }
            if (maxPrice != null && product.getPrice() != null && product.getPrice().compareTo(maxPrice) > 0) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.set("id", product.getId());
            item.set("name", product.getName());
            item.set("price", product.getPrice());
            item.set("originalPrice", product.getOriginalPrice());
            item.set("tags", product.getTags());
            item.set("sellingPoint", product.getSellingPoint());
            array.add(item);
            // 最多返回 8 条候选，控制上下文长度
            if (++count >= 8) {
                break;
            }
        }
        if (array.isEmpty()) {
            return "未检索到符合条件的上架商品。";
        }
        return array.toString();
    }

    private ProductToolRequest productRequest(Integer productId) {
        ProductToolRequest request = new ProductToolRequest();
        request.setProductId(productId);
        request.setQuantity(1);
        return request;
    }

    // 安全解析模型返回的工具参数（可能是 JSON 字符串）
    private JSONObject parseArguments(String argumentsRaw) {
        if (ObjectUtil.isEmpty(argumentsRaw)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(argumentsRaw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    // 解析 submit_recommendations 的 items 参数
    private List<AgentSubmission> parseSubmissions(JSONObject args) {
        List<AgentSubmission> list = new ArrayList<>();
        JSONArray items = args.getJSONArray("items");
        if (items == null) {
            return list;
        }
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            Integer productId = item.getInt("productId");
            if (productId == null) {
                continue;
            }
            list.add(new AgentSubmission(productId, item.getStr("reason")));
        }
        return list;
    }

    // 工具英文名到中文步骤名的映射，用于 AgentStep 展示
    private String toolLabel(String name) {
        switch (name) {
            case "search_candidate_products":
                return "检索候选商品";
            case "query_product_price":
                return "查询商品价格";
            case "query_product_stock":
                return "查询商品库存";
            case "query_product_promotion":
                return "查询商品优惠";
            case "query_user_profile":
                return "读取用户画像";
            case "submit_recommendations":
                return "提交推荐结果";
            default:
                return name;
        }
    }

    // 承载模型提交的一条推荐（商品ID + 推荐理由）
    private static class AgentSubmission {
        private final Integer productId;
        private final String reason;

        private AgentSubmission(Integer productId, String reason) {
            this.productId = productId;
            this.reason = reason;
        }
    }

    private void validate(ShoppingGuideTask shoppingGuideTask) {
        if (ObjectUtil.isNull(shoppingGuideTask)
                || ObjectUtil.isEmpty(shoppingGuideTask.getDemandText())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }

    private BusinessToolResult queryUserProfile(ShoppingGuideTask task) {
        if (ObjectUtil.isEmpty(task.getUserId())) {
            return null;
        }
        BusinessToolRequest request = new BusinessToolRequest();
        request.setUserId(task.getUserId());
        return businessToolService.queryUserProfile(request);
    }

    private Integer scoreProduct(Product product) {
        int score = 0;
        if (product.getIsRecommend() != null && product.getIsRecommend() == 1) {
            score += 20;
        }
        if (product.getSort() != null) {
            score += Math.max(0, 20 - product.getSort());
        }
        if (product.getOriginalPrice() != null && product.getPrice() != null
                && product.getOriginalPrice().compareTo(product.getPrice()) > 0) {
            score += 10;
        }
        return score;
    }

    private String buildRecommendBlock(Product product,
                                       ProductToolResult priceResult,
                                       ProductToolResult stockResult,
                                       ProductToolResult promotionResult,
                                       String recommendReason) {
        StringBuilder builder = new StringBuilder();
        builder.append("推荐商品：").append(product.getName()).append("\n");
        builder.append("商品编号：").append(product.getProductNo()).append("\n");
        builder.append("当前价格：").append(priceResult.getPrice()).append("，原价：").append(priceResult.getOriginalPrice()).append("\n");
        builder.append("库存数量：").append(stockResult.getStockQuantity()).append("\n");
        builder.append("优惠信息：优惠 ").append(promotionResult.getDiscountAmount())
                .append("，折扣率 ").append(promotionResult.getDiscountRate()).append("\n");
        builder.append("推荐理由：").append(recommendReason);
        return builder.toString();
    }

    private ShoppingRecommendation buildRecommendation(ShoppingGuideTask task,
                                                       AgentRun agentRun,
                                                       Product product,
                                                       ProductToolResult priceResult,
                                                       ProductToolResult stockResult,
                                                       ProductToolResult promotionResult,
                                                       String recommendReason,
                                                       Integer rank) {
        ShoppingRecommendation recommendation = new ShoppingRecommendation();
        recommendation.setTaskId(task.getId());
        recommendation.setRunId(agentRun.getId());
        recommendation.setUserId(task.getUserId());
        recommendation.setProductId(product.getId());
        recommendation.setProductNo(product.getProductNo());
        recommendation.setProductName(product.getName());
        recommendation.setProductImage(product.getCoverImage());
        recommendation.setPriceSnapshot(priceResult.getPrice());
        recommendation.setOriginalPriceSnapshot(priceResult.getOriginalPrice());
        recommendation.setDiscountAmount(promotionResult.getDiscountAmount());
        recommendation.setDiscountRate(promotionResult.getDiscountRate());
        // 记录推荐当时的商品库存，方便回看这条推荐是不是在有货时给出的
        recommendation.setAvailableQuantity(stockResult.getStockQuantity());
        recommendation.setRecommendRank(rank);
        recommendation.setRecommendScore(calculateRecommendScore(product, stockResult, promotionResult, task));
        recommendation.setRecommendReason(recommendReason);
        recommendation.setEvidenceSummary(buildEvidenceSummary(product, priceResult, stockResult, promotionResult));
        return recommendation;
    }

    private Integer calculateRecommendScore(Product product,
                                            ProductToolResult stockResult,
                                            ProductToolResult promotionResult,
                                            ShoppingGuideTask task) {
        int score = scoreProduct(product);
        if (stockResult.getStockQuantity() != null && stockResult.getStockQuantity() > 0) {
            score += 30;
        }
        if (promotionResult.getDiscountAmount() != null && promotionResult.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            score += 15;
        }
        if (task.getBudgetAmount() != null && product.getPrice() != null && product.getPrice().compareTo(task.getBudgetAmount()) <= 0) {
            score += 20;
        }
        return score;
    }

    private String buildEvidenceSummary(Product product,
                                        ProductToolResult priceResult,
                                        ProductToolResult stockResult,
                                        ProductToolResult promotionResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("商品状态：").append(product.getStatus()).append("\n");
        builder.append("当前价格：").append(priceResult.getPrice()).append("\n");
        builder.append("库存数量：").append(stockResult.getStockQuantity()).append("\n");
        builder.append("优惠金额：").append(promotionResult.getDiscountAmount()).append("\n");
        builder.append("商品标签：").append(product.getTags());
        return builder.toString();
    }
}
