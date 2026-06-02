package com.dz.couple.module.agent.mapper;

import com.dz.couple.module.agent.entity.AgentInteraction;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentInteractionMapper {
    @Insert("INSERT INTO agent_interaction (couple_id, user_id, interaction_type, intent, user_message, agent_response, tools_used, emotion_score, created_at) " +
            "VALUES (#{coupleId}, #{userId}, #{interactionType}, #{intent}, #{userMessage}, #{agentResponse}, #{toolsUsed}, #{emotionScore}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentInteraction interaction);

    @Select("SELECT * FROM agent_interaction WHERE couple_id = #{coupleId} ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentInteraction> findRecentByCouple(@Param("coupleId") Long coupleId, @Param("limit") int limit);

    @Select("SELECT * FROM agent_interaction WHERE couple_id = #{coupleId} AND interaction_type = #{type} ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentInteraction> findRecentByType(@Param("coupleId") Long coupleId,
                                            @Param("type") String type,
                                            @Param("limit") int limit);

    /** 删除单条交互记录（仅允许删除自己的消息） */
    @Delete("DELETE FROM agent_interaction WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    /** 批量删除交互记录 */
    @Delete("<script>" +
            "DELETE FROM agent_interaction WHERE user_id = #{userId} AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /** 清空某用户的所有对话记录 */
    @Delete("DELETE FROM agent_interaction WHERE user_id = #{userId}")
    int deleteAllByUser(@Param("userId") Long userId);

    /** 分页查询用户历史对话 */
    @Select("<script>" +
            "SELECT * FROM agent_interaction WHERE user_id = #{userId} " +
            "<if test='beforeId != null'>AND id <![CDATA[ < ]]> #{beforeId} </if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<AgentInteraction> findHistory(@Param("userId") Long userId,
                                       @Param("beforeId") Long beforeId,
                                       @Param("limit") int limit);
}
