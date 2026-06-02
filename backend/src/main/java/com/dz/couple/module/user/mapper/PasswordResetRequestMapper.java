package com.dz.couple.module.user.mapper;

import com.dz.couple.module.user.entity.PasswordResetRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface PasswordResetRequestMapper {
    @Insert("insert into t_password_reset_request(user_id, couple_id, code_hash, expires_at, used_flag, created_at, updated_at) " +
            "values(#{userId}, #{coupleId}, #{codeHash}, #{expiresAt}, 0, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PasswordResetRequest req);

    @Select("select id, user_id as userId, couple_id as coupleId, code_hash as codeHash, expires_at as expiresAt, used_flag as usedFlag, created_at as createdAt, updated_at as updatedAt " +
            "from t_password_reset_request where user_id = #{userId} and used_flag = 0 and expires_at > #{now} order by id desc limit 1")
    PasswordResetRequest findLatestValid(@Param("userId") Long userId, @Param("now") Date now);

    @Update("update t_password_reset_request set used_flag = 1, updated_at = now() where id = #{id}")
    int markUsed(@Param("id") Long id);
}
