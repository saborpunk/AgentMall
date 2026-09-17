package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductKnowledge;
import com.example.entity.ProductKnowledgeChunk;
import com.example.exception.CustomException;
import com.example.mapper.ProductKnowledgeChunkMapper;
import com.example.mapper.ProductKnowledgeEmbeddingMapper;
import com.example.mapper.ProductKnowledgeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductKnowledgeChunkService {

    private static final int CHUNK_SIZE = 180;
    private static final int OVERLAP_SIZE = 30;

    @Resource
    private ProductKnowledgeMapper productKnowledgeMapper;
    @Resource
    private ProductKnowledgeChunkMapper productKnowledgeChunkMapper;
    @Resource
    private ProductKnowledgeEmbeddingMapper productKnowledgeEmbeddingMapper;

    public int generateByKnowledgeId(Integer knowledgeId) {
        if (ObjectUtil.isEmpty(knowledgeId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        ProductKnowledge knowledge = productKnowledgeMapper.selectById(knowledgeId);
        if (ObjectUtil.isNull(knowledge) || !ObjectUtil.equal(knowledge.getIsEnabled(), 1)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        productKnowledgeEmbeddingMapper.deleteByKnowledgeId(knowledgeId);
        productKnowledgeChunkMapper.deleteByKnowledgeId(knowledgeId);
        return saveChunks(knowledge);
    }

    public int generateAll() {
        ProductKnowledge condition = new ProductKnowledge();
        condition.setIsEnabled(1);
        List<ProductKnowledge> knowledgeList = productKnowledgeMapper.selectAll(condition);
        int count = 0;
        for (ProductKnowledge knowledge : knowledgeList) {
            productKnowledgeEmbeddingMapper.deleteByKnowledgeId(knowledge.getId());
            productKnowledgeChunkMapper.deleteByKnowledgeId(knowledge.getId());
            count += saveChunks(knowledge);
        }
        return count;
    }

    public void deleteById(Integer id) {
        productKnowledgeEmbeddingMapper.deleteByChunkId(id);
        productKnowledgeChunkMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            productKnowledgeEmbeddingMapper.deleteByChunkId(id);
        }
        productKnowledgeChunkMapper.deleteBatch(ids);
    }

    public List<ProductKnowledgeChunk> selectAll(ProductKnowledgeChunk productKnowledgeChunk) {
        return productKnowledgeChunkMapper.selectAll(productKnowledgeChunk);
    }

    public PageInfo<ProductKnowledgeChunk> selectPage(ProductKnowledgeChunk productKnowledgeChunk, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductKnowledgeChunk> list = productKnowledgeChunkMapper.selectAll(productKnowledgeChunk);
        return PageInfo.of(list);
    }

    private int saveChunks(ProductKnowledge knowledge) {
        List<String> chunks = splitText(buildKnowledgeText(knowledge));
        String now = DateUtil.now();
        for (int i = 0; i < chunks.size(); i++) {
            ProductKnowledgeChunk chunk = new ProductKnowledgeChunk();
            chunk.setKnowledgeId(knowledge.getId());
            chunk.setProductId(knowledge.getProductId());
            chunk.setKnowledgeType(knowledge.getKnowledgeType());
            chunk.setChunkNo(i + 1);
            chunk.setChunkTitle(knowledge.getTitle() + " - 片段" + (i + 1));
            chunk.setChunkContent(chunks.get(i));
            chunk.setCharacterCount(chunks.get(i).length());
            chunk.setChunkStatus("READY");
            chunk.setCreateTime(now);
            chunk.setUpdateTime(now);
            productKnowledgeChunkMapper.insert(chunk);
        }
        return chunks.size();
    }

    private String buildKnowledgeText(ProductKnowledge knowledge) {
        return "资料标题：" + knowledge.getTitle() + "\n"
                + "知识类型：" + knowledge.getKnowledgeType() + "\n"
                + "资料内容：" + knowledge.getContent();
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (ObjectUtil.isEmpty(text)) {
            return chunks;
        }
        String content = text.trim();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            chunks.add(content.substring(start, end));
            if (end == content.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_SIZE, start + 1);
        }
        return chunks;
    }
}
