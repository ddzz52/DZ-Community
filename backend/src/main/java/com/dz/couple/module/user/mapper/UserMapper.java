package com.dz.couple.module.user.mapper;

import com.dz.couple.module.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select id, couple_id as coupleId, username, password_hash as passwordHash, nickname, avatar_url as avatarUrl, gender, love_date as loveDate, zodiac, signature, temp_signature as tempSignature, signature_expire_time as signatureExpireTime, created_at as createdAt, updated_at as updatedAt " +
            "from t_user where id = #{id}")
    User findById(@Param("id") Long id);

    @Select("select id, couple_id as coupleId, username, password_hash as passwordHash, nickname, avatar_url as avatarUrl, gender, love_date as loveDate, signature, temp_signature as tempSignature, signature_expire_time as signatureExpireTime, created_at as createdAt, updated_at as updatedAt " +
            "from t_user where username = #{username} limit 1")
    User findByUsername(@Param("username") String username);

    @Select("select count(1) from t_user")
    long countAll();

    @Select("select count(1) from t_user where couple_id = #{coupleId}")
    long countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, username, password_hash as passwordHash, nickname, avatar_url as avatarUrl, gender, love_date as loveDate, signature, temp_signature as tempSignature, signature_expire_time as signatureExpireTime, created_at as createdAt, updated_at as updatedAt " +
            "from t_user order by id asc limit 1")
    User findFirstUser();

    @Insert("insert into t_user(couple_id, username, password_hash, nickname, avatar_url, gender, love_date, signature, temp_signature, signature_expire_time, created_at, updated_at) " +
            "values(#{coupleId}, #{username}, #{passwordHash}, #{nickname}, #{avatarUrl}, #{gender}, #{loveDate}, #{signature}, #{tempSignature}, #{signatureExpireTime}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("select id, couple_id as coupleId, username, password_hash as passwordHash, nickname, avatar_url as avatarUrl, gender, love_date as loveDate, signature, temp_signature as tempSignature, signature_expire_time as signatureExpireTime, created_at as createdAt, updated_at as updatedAt " +
            "from t_user where couple_id = #{coupleId} order by id asc")
    List<User> listByCoupleId(@Param("coupleId") Long coupleId);

    @Update("update t_user set nickname = #{nickname}, avatar_url = #{avatarUrl}, gender = #{gender}, love_date = #{loveDate}, zodiac = #{zodiac}, updated_at = now() where id = #{id}")
    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname, @Param("avatarUrl") String avatarUrl, @Param("gender") Integer gender, @Param("loveDate") java.util.Date loveDate, @Param("zodiac") String zodiac);

    @Update("update t_user set signature = #{signature}, updated_at = now() where id = #{id}")
    int updateSignature(@Param("id") Long id, @Param("signature") String signature);

    @Update("update t_user set temp_signature = #{tempSignature}, signature_expire_time = #{signatureExpireTime}, updated_at = now() where id = #{id}")
    int updateTempSignature(@Param("id") Long id, @Param("tempSignature") String tempSignature, @Param("signatureExpireTime") java.util.Date signatureExpireTime);

    @Update("update t_user set password_hash = #{passwordHash}, updated_at = now() where id = #{id}")
    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
