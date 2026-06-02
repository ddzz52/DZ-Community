package com.dz.couple.module.period.mapper;

import com.dz.couple.module.period.entity.PeriodSettings;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PeriodSettingsMapper {
    @Select("select id, couple_id as coupleId, owner_user_id as ownerUserId, enc_data as encData, created_by as createdBy, updated_by as updatedBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_period_settings where couple_id = #{coupleId} limit 1")
    PeriodSettings findByCoupleId(@Param("coupleId") Long coupleId);

    @Insert("insert into t_period_settings(couple_id, owner_user_id, enc_data, created_by, updated_by, created_at, updated_at) " +
            "values(#{coupleId}, #{ownerUserId}, #{encData}, #{createdBy}, #{updatedBy}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PeriodSettings s);

    @Update("update t_period_settings set owner_user_id = #{ownerUserId}, enc_data = #{encData}, updated_by = #{updatedBy}, updated_at = now() " +
            "where couple_id = #{coupleId}")
    int updateByCoupleId(PeriodSettings s);
}

