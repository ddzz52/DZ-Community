package com.dz.couple.module.agent.mapper;

import com.dz.couple.module.agent.entity.AgentMemory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AgentMemoryMapper {
    @Insert("INSERT INTO agent_memory (couple_id, memory_type, memory_key, memory_value, importance, expire_at, created_at, updated_at) " +
            "VALUES (#{coupleId}, #{memoryType}, #{memoryKey}, #{memoryValue}, #{importance}, #{expireAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentMemory memory);

    @Select("SELECT * FROM agent_memory WHERE couple_id = #{coupleId} AND memory_type = #{type} AND memory_key = #{key}")
    AgentMemory findByCoupleAndKey(@Param("coupleId") Long coupleId,
                                   @Param("type") String type,
                                   @Param("key") String key);

    @Select("SELECT * FROM agent_memory WHERE couple_id = #{coupleId} AND memory_type = #{type}")
    List<AgentMemory> findByCoupleAndType(@Param("coupleId") Long coupleId,
                                          @Param("type") String type);

    @Select("SELECT * FROM agent_memory " +
            "WHERE couple_id = #{coupleId} AND memory_type = #{type} " +
            "AND (expire_at IS NULL OR expire_at > NOW()) " +
            "ORDER BY importance DESC, updated_at DESC " +
            "LIMIT #{limit}")
    List<AgentMemory> listValidByType(@Param("coupleId") Long coupleId,
                                      @Param("type") String type,
                                      @Param("limit") int limit);

    @Update("UPDATE agent_memory SET memory_value = #{memoryValue}, importance = #{importance}, updated_at = NOW() " +
            "WHERE couple_id = #{coupleId} AND memory_type = #{type} AND memory_key = #{key}")
    int updateByCoupleAndKey(@Param("coupleId") Long coupleId,
                             @Param("type") String type,
                             @Param("key") String key,
                             @Param("memoryValue") String memoryValue,
                             @Param("importance") Integer importance);

    @Insert("INSERT INTO agent_memory (couple_id, memory_type, memory_key, memory_value, importance, expire_at, created_at, updated_at) " +
            "VALUES (#{coupleId}, #{type}, #{key}, #{memoryValue}, #{importance}, #{expireAt}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "memory_value = VALUES(memory_value), " +
            "importance = VALUES(importance), " +
            "expire_at = VALUES(expire_at), " +
            "updated_at = NOW()")
    int upsert(@Param("coupleId") Long coupleId,
               @Param("type") String type,
               @Param("key") String key,
               @Param("memoryValue") String memoryValue,
               @Param("importance") Integer importance,
               @Param("expireAt") java.util.Date expireAt);

    @Delete("DELETE FROM agent_memory WHERE id = #{id}")
    int deleteById(Long id);
}

