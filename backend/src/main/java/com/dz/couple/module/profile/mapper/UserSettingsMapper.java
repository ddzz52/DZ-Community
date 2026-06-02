package com.dz.couple.module.profile.mapper;

import com.dz.couple.module.profile.entity.UserSettings;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserSettingsMapper {
    @Select("select user_id as userId, couple_id as coupleId, message_enabled as messageEnabled, reminder_enabled as reminderEnabled, reminder_window_start as reminderWindowStart, reminder_window_end as reminderWindowEnd, dnd_enabled as dndEnabled, dnd_start as dndStart, dnd_end as dndEnd, created_at as createdAt, updated_at as updatedAt " +
            "from t_user_settings where user_id = #{userId}")
    UserSettings findByUserId(@Param("userId") Long userId);

    @Insert("insert into t_user_settings(user_id, couple_id, message_enabled, reminder_enabled, reminder_window_start, reminder_window_end, dnd_enabled, dnd_start, dnd_end, created_at, updated_at) " +
            "values(#{userId}, #{coupleId}, #{messageEnabled}, #{reminderEnabled}, #{reminderWindowStart}, #{reminderWindowEnd}, #{dndEnabled}, #{dndStart}, #{dndEnd}, now(), now())")
    int insert(UserSettings settings);

    @Update("update t_user_settings set message_enabled = #{messageEnabled}, reminder_enabled = #{reminderEnabled}, reminder_window_start = #{reminderWindowStart}, reminder_window_end = #{reminderWindowEnd}, dnd_enabled = #{dndEnabled}, dnd_start = #{dndStart}, dnd_end = #{dndEnd}, updated_at = now() where user_id = #{userId}")
    int update(UserSettings settings);
}
