package com.example.mapper;

import com.example.entity.ProductKnowledgeEmbedding;

import java.util.List;

public interface ProductKnowledgeEmbeddingMapper {

    int insert(ProductKnowledgeEmbedding productKnowledgeEmbedding);

    void deleteById(Integer id);

    void deleteByChunkId(Integer chunkId);

    void deleteByKnowledgeId(Integer knowledgeId);

    void deleteBatch(List<Integer> ids);

    List<ProductKnowledgeEmbedding> selectAll(ProductKnowledgeEmbedding productKnowledgeEmbedding);
}
