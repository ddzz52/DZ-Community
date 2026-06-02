package com.dz.couple.module.message.mapper;

import com.dz.couple.module.message.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface MessageMapper {
    @Insert("insert into t_message(couple_id, sender_id, msg_type, content, delivered_at, read_at, recalled_flag, recalled_by, recalled_at, created_at) " +
            "values(#{coupleId}, #{senderId}, #{msgType}, #{content}, #{deliveredAt}, #{readAt}, #{recalledFlag}, #{recalledBy}, #{recalledAt}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message m);

    @Select("<script>" +
            "select id, couple_id as coupleId, sender_id as senderId, msg_type as msgType, content, delivered_at as deliveredAt, read_at as readAt, recalled_flag as recalledFlag, recalled_by as recalledBy, recalled_at as recalledAt, created_at as createdAt " +
            "from t_message where couple_id = #{coupleId} " +
            "<if test='beforeId != null'>and id <![CDATA[ < ]]> #{beforeId} </if>" +
            "order by id desc limit #{limit}" +
            "</script>")
    List<Message> list(@Param("coupleId") Long coupleId, @Param("beforeId") Long beforeId, @Param("limit") int limit);

    @Select("select id, couple_id as coupleId, sender_id as senderId, msg_type as msgType, content, delivered_at as deliveredAt, read_at as readAt, recalled_flag as recalledFlag, recalled_by as recalledBy, recalled_at as recalledAt, created_at as createdAt " +
            "from t_message where couple_id = #{coupleId} and id = #{id} limit 1")
    Message findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("<script>" +
            "select id, couple_id as coupleId, sender_id as senderId, msg_type as msgType, content, delivered_at as deliveredAt, read_at as readAt, recalled_flag as recalledFlag, recalled_by as recalledBy, recalled_at as recalledAt, created_at as createdAt " +
            "from t_message where couple_id = #{coupleId} and id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Message> listByIds(@Param("coupleId") Long coupleId, @Param("ids") List<Long> ids);

    @Select("select count(1) from t_message where couple_id = #{coupleId} and sender_id <> #{userId} and read_at is null and recalled_flag = 0")
    int countUnread(@Param("coupleId") Long coupleId, @Param("userId") Long userId);

    @Update("update t_message set read_at = #{now} where couple_id = #{coupleId} and sender_id <> #{userId} and read_at is null and id <= #{upToId}")
    int markReadUpTo(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("upToId") Long upToId, @Param("now") Date now);

    @Update("update t_message set delivered_at = #{now} where couple_id = #{coupleId} and sender_id <> #{userId} and delivered_at is null and id <= #{upToId}")
    int markDeliveredUpTo(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("upToId") Long upToId, @Param("now") Date now);

    @Update("update t_message set delivered_at = #{now} where couple_id = #{coupleId} and id = #{id} and sender_id <> #{userId} and delivered_at is null and recalled_flag = 0")
    int markDelivered(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("id") Long id, @Param("now") Date now);

    @Update("update t_message set read_at = #{now} where couple_id = #{coupleId} and id = #{id} and sender_id <> #{userId} and read_at is null and recalled_flag = 0")
    int markRead(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("id") Long id, @Param("now") Date now);

    @Update("update t_message set recalled_flag = 1, recalled_by = #{userId}, recalled_at = #{now}, content = '', " +
            "delivered_at = ifnull(delivered_at, #{now}), read_at = ifnull(read_at, #{now}) " +
            "where couple_id = #{coupleId} and id = #{id} and sender_id = #{userId} and recalled_flag = 0 and created_at >= #{cutoff}")
    int recall(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("id") Long id, @Param("cutoff") Date cutoff, @Param("now") Date now);

    @Delete("delete from t_message where couple_id = #{coupleId} and id = #{id}")
    int deleteById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Delete("<script>" +
            "delete from t_message where couple_id = #{coupleId} and id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteBatch(@Param("coupleId") Long coupleId, @Param("ids") List<Long> ids);

    @Delete("delete from t_message where couple_id = #{coupleId}")
    int deleteAllByCouple(@Param("coupleId") Long coupleId);
}
