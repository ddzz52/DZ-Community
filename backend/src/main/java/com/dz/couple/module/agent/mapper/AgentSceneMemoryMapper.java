package com.dz.couple.module.agent.mapper;

import com.dz.couple.module.agent.entity.AgentSceneMemory;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentSceneMemoryMapper {

    @Insert("INSERT INTO agent_scene_memory (couple_id, user_id, memory_type, content, raw_data, related_intent, importance, expire_at, created_at) " +
            "VALUES (#{coupleId}, #{userId}, #{memoryType}, #{content}, #{rawData}, #{relatedIntent}, #{importance}, #{expireAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentSceneMemory memory);

    @Select("SELECT * FROM agent_scene_memory WHERE couple_id = #{coupleId} AND memory_type = #{type} " +
            "AND (expire_at IS NULL OR expire_at > NOW()) ORDER BY importance DESC, created_at DESC")
    List<AgentSceneMemory> findValidByCoupleAndType(@Param("coupleId") Long coupleId,
                                                     @Param("type") String type);

    @Select("SELECT * FROM agent_scene_memory WHERE couple_id = #{coupleId} " +
            "AND (expire_at IS NULL OR expire_at > NOW()) ORDER BY importance DESC, created_at DESC LIMIT #{limit}")
    List<AgentSceneMemory> listValidByCoupleId(@Param("coupleId") Long coupleId,
                                                @Param("limit") int limit);

    @Select("SELECT * FROM agent_scene_memory WHERE id = #{id}")
    AgentSceneMemory findById(Long id);

    @Update("UPDATE agent_scene_memory SET importance = #{importance} WHERE id = #{id}")
    int updateImportance(@Param("id") Long id, @Param("importance") Integer importance);

    @Delete("DELETE FROM agent_scene_memory WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM agent_scene_memory WHERE couple_id = #{coupleId} AND expire_at IS NOT NULL AND expire_at <= #{now}")
    int deleteExpired(@Param("coupleId") Long coupleId, @Param("now") LocalDateTime now);

    @Delete("DELETE FROM agent_scene_memory WHERE created_at < #{before}")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
