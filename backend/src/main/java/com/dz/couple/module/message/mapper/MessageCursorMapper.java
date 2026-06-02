package com.dz.couple.module.message.mapper;

import com.dz.couple.module.message.entity.MessageCursor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageCursorMapper {
    @Select("select user_id as userId, couple_id as coupleId, device_id as deviceId, last_read_id as lastReadId, updated_at as updatedAt " +
            "from t_message_cursor where user_id = #{userId} and device_id = #{deviceId} limit 1")
    MessageCursor find(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Insert("insert into t_message_cursor(user_id, couple_id, device_id, last_read_id, updated_at) " +
            "values(#{userId}, #{coupleId}, #{deviceId}, #{lastReadId}, now()) " +
            "on duplicate key update couple_id = values(couple_id), " +
            "last_read_id = case when last_read_id is null or last_read_id < values(last_read_id) then values(last_read_id) else last_read_id end, " +
            "updated_at = now()")
    int upsert(MessageCursor c);
}

