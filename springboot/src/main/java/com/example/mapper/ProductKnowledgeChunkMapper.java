package com.example.mapper;

import com.example.entity.ProductKnowledgeChunk;

import java.util.List;

public interface ProductKnowledgeChunkMapper {

    int insert(ProductKnowledgeChunk productKnowledgeChunk);

    void deleteById(Integer id);

    void deleteByKnowledgeId(Integer knowledgeId);

    void deleteBatch(List<Integer> ids);

    List<ProductKnowledgeChunk> selectAll(ProductKnowledgeChunk productKnowledgeChunk);
}
