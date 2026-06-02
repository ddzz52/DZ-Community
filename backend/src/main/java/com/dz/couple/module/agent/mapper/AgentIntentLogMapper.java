package com.dz.couple.module.agent.mapper;

import com.dz.couple.module.agent.entity.AgentIntentLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 意图识别日志 Mapper
 */
@Mapper
public interface AgentIntentLogMapper {

    @Insert("INSERT INTO agent_intent_log (couple_id, user_id, message, recognized_intent, params_json, "
            + "confidence, source, latency_ms, created_at) "
            + "VALUES (#{coupleId}, #{userId}, #{message}, #{recognizedIntent}, #{paramsJson}, "
            + "#{confidence}, #{source}, #{latencyMs}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentIntentLog log);

    @Select("SELECT * FROM agent_intent_log WHERE couple_id = #{coupleId} ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentIntentLog> listByCoupleId(@Param("coupleId") Long coupleId, @Param("limit") int limit);

    @Select("SELECT * FROM agent_intent_log WHERE couple_id = #{coupleId} AND created_at >= #{since} "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentIntentLog> listByCoupleIdSince(@Param("coupleId") Long coupleId,
                                              @Param("since") String since,
                                              @Param("limit") int limit);

    @Select("SELECT recognized_intent, COUNT(*) as cnt FROM agent_intent_log "
            + "WHERE couple_id = #{coupleId} AND created_at >= #{since} "
            + "GROUP BY recognized_intent ORDER BY cnt DESC")
    List<IntentStats> statsByCoupleId(@Param("coupleId") Long coupleId, @Param("since") String since);

    @Select("SELECT source, COUNT(*) as cnt FROM agent_intent_log "
            + "WHERE created_at >= #{since} GROUP BY source")
    List<IntentStats> statsBySource(@Param("since") String since);

    @Select("SELECT AVG(latency_ms) FROM agent_intent_log WHERE source = 'LLM' AND created_at >= #{since}")
    Double avgLlmLatency(@Param("since") String since);

    /** 内部统计结果 */
    class IntentStats {
        private String recognizedIntent;
        private String source;
        private long cnt;

        public String getRecognizedIntent() { return recognizedIntent; }
        public void setRecognizedIntent(String recognizedIntent) { this.recognizedIntent = recognizedIntent; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public long getCnt() { return cnt; }
        public void setCnt(long cnt) { this.cnt = cnt; }
    }
}
