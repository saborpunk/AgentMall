package com.example.mapper;

import com.example.entity.AgentRun;
import java.util.List;

public interface AgentRunMapper {

    int insert(AgentRun agentRun);

    void updateById(AgentRun agentRun);

    void deleteById(Integer id);

    List<AgentRun> selectAll(AgentRun agentRun);
}
