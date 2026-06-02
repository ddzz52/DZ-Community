package com.dz.couple.module.agent.mapper;

import com.dz.couple.module.agent.entity.AgentCoreMemory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentCoreMemoryMapper {

    @Insert("INSERT INTO agent_core_memory (couple_id, memory_type, memory_key, memory_value, importance, source, created_at, updated_at) " +
            "VALUES (#{coupleId}, #{memoryType}, #{memoryKey}, #{memoryValue}, #{importance}, #{source}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentCoreMemory memory);

    @Select("SELECT * FROM agent_core_memory WHERE couple_id = #{coupleId} AND memory_type = #{type} AND memory_key = #{key}")
    AgentCoreMemory findByCoupleAndKey(@Param("coupleId") Long coupleId,
                                       @Param("type") String type,
                                       @Param("key") String key);

    @Select("SELECT * FROM agent_core_memory WHERE couple_id = #{coupleId} AND memory_type = #{type} ORDER BY importance DESC, updated_at DESC")
    List<AgentCoreMemory> findByCoupleAndType(@Param("coupleId") Long coupleId,
                                              @Param("type") String type);

    @Select("SELECT * FROM agent_core_memory WHERE couple_id = #{coupleId} ORDER BY importance DESC, updated_at DESC LIMIT #{limit}")
    List<AgentCoreMemory> listByCoupleId(@Param("coupleId") Long coupleId,
                                         @Param("limit") int limit);

    @Update("UPDATE agent_core_memory SET memory_value = #{memoryValue}, importance = #{importance}, source = #{source}, updated_at = NOW() " +
            "WHERE couple_id = #{coupleId} AND memory_type = #{type} AND memory_key = #{key}")
    int updateByCoupleAndKey(@Param("coupleId") Long coupleId,
                             @Param("type") String type,
                             @Param("key") String key,
                             @Param("memoryValue") String memoryValue,
                             @Param("importance") Integer importance,
                             @Param("source") String source);

    @Insert("INSERT INTO agent_core_memory (couple_id, memory_type, memory_key, memory_value, importance, source, created_at, updated_at) " +
            "VALUES (#{coupleId}, #{type}, #{key}, #{memoryValue}, #{importance}, #{source}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "memory_value = VALUES(memory_value), " +
            "importance = VALUES(importance), " +
            "source = VALUES(source), " +
            "updated_at = NOW()")
    int upsert(@Param("coupleId") Long coupleId,
               @Param("type") String type,
               @Param("key") String key,
               @Param("memoryValue") String memoryValue,
               @Param("importance") Integer importance,
               @Param("source") String source);

    @Delete("DELETE FROM agent_core_memory WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM agent_core_memory WHERE couple_id = #{coupleId} AND memory_key = #{key}")
    int deleteByCoupleAndKey(@Param("coupleId") Long coupleId, @Param("key") String key);
}
