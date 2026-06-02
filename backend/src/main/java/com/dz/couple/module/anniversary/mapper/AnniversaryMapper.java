package com.dz.couple.module.anniversary.mapper;

import com.dz.couple.module.anniversary.entity.Anniversary;
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
public interface AnniversaryMapper {

    @Select("select id, couple_id as coupleId, title, anniversary_date as anniversaryDate, calendar_type as calendarType, lunar_month as lunarMonth, lunar_day as lunarDay, lunar_leap as lunarLeap, type, icon, theme_color as themeColor, cover_url as coverUrl, cover_thumb_url as coverThumbUrl, note, reminder_enabled as reminderEnabled, reminder_days_before as reminderDaysBefore, reminder_on_day as reminderOnDay, pinned, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_anniversary where couple_id = #{coupleId} and pinned = 1 order by updated_at desc limit 1")
    Anniversary findPinned(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, title, anniversary_date as anniversaryDate, calendar_type as calendarType, lunar_month as lunarMonth, lunar_day as lunarDay, lunar_leap as lunarLeap, type, icon, theme_color as themeColor, cover_url as coverUrl, cover_thumb_url as coverThumbUrl, note, reminder_enabled as reminderEnabled, reminder_days_before as reminderDaysBefore, reminder_on_day as reminderOnDay, pinned, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_anniversary where couple_id = #{coupleId} order by anniversary_date asc")
    List<Anniversary> listAll(@Param("coupleId") Long coupleId);

    @Select("select count(1) from t_anniversary where couple_id = #{coupleId}")
    long countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, title, anniversary_date as anniversaryDate, calendar_type as calendarType, lunar_month as lunarMonth, lunar_day as lunarDay, lunar_leap as lunarLeap, type, icon, theme_color as themeColor, cover_url as coverUrl, cover_thumb_url as coverThumbUrl, note, reminder_enabled as reminderEnabled, reminder_days_before as reminderDaysBefore, reminder_on_day as reminderOnDay, pinned, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_anniversary where couple_id = #{coupleId} and id = #{id} limit 1")
    Anniversary findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Insert("insert into t_anniversary(couple_id, title, anniversary_date, calendar_type, lunar_month, lunar_day, lunar_leap, type, icon, theme_color, cover_url, cover_thumb_url, note, reminder_enabled, reminder_days_before, reminder_on_day, pinned, created_by, created_at, updated_at) " +
            "values(#{coupleId}, #{title}, #{anniversaryDate}, #{calendarType}, #{lunarMonth}, #{lunarDay}, #{lunarLeap}, #{type}, #{icon}, #{themeColor}, #{coverUrl}, #{coverThumbUrl}, #{note}, #{reminderEnabled}, #{reminderDaysBefore}, #{reminderOnDay}, #{pinned}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Anniversary a);

    @Update("update t_anniversary set title = #{title}, anniversary_date = #{anniversaryDate}, calendar_type = #{calendarType}, lunar_month = #{lunarMonth}, lunar_day = #{lunarDay}, lunar_leap = #{lunarLeap}, type = #{type}, icon = #{icon}, theme_color = #{themeColor}, cover_url = #{coverUrl}, cover_thumb_url = #{coverThumbUrl}, note = #{note}, reminder_enabled = #{reminderEnabled}, reminder_days_before = #{reminderDaysBefore}, reminder_on_day = #{reminderOnDay}, updated_at = #{updatedAt} " +
            "where couple_id = #{coupleId} and id = #{id}")
    int update(Anniversary a);

    @Delete("delete from t_anniversary where couple_id = #{coupleId} and id = #{id}")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Update("update t_anniversary set pinned = 0, updated_at = #{now} where couple_id = #{coupleId} and pinned = 1")
    int clearPinned(@Param("coupleId") Long coupleId, @Param("now") Date now);

    @Update("update t_anniversary set pinned = 1, updated_at = #{now} where couple_id = #{coupleId} and id = #{id}")
    int setPinned(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("now") Date now);

    @Update("update t_anniversary set pinned = 0, updated_at = #{now} where couple_id = #{coupleId} and id = #{id}")
    int unsetPinned(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("now") Date now);
}
