package com.dz.couple.module.memo.mapper;

import com.dz.couple.module.memo.dto.MemoVO;
import com.dz.couple.module.memo.entity.Memo;
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
public interface MemoMapper {
    @Select("<script>" +
            "select m.id, m.couple_id as coupleId, m.category_id as categoryId, c.name as categoryName, m.title, m.content, m.status, " +
            "m.created_by as createdBy, uc.nickname as createdByNickname, m.updated_by as updatedBy, uu.nickname as updatedByNickname, m.created_at as createdAt, m.updated_at as updatedAt " +
            "from t_memo m " +
            "left join t_memo_category c on c.id = m.category_id " +
            "left join t_user uc on uc.id = m.created_by " +
            "left join t_user uu on uu.id = m.updated_by " +
            "where m.couple_id = #{coupleId} " +
            "<if test='categoryId != null'>and m.category_id = #{categoryId} </if>" +
            "<if test='status != null'>and m.status = #{status} </if>" +
            "<if test='q != null and q.trim() != \"\"'>and (m.title like concat('%', #{q}, '%') or m.content like concat('%', #{q}, '%')) </if>" +
            "<if test='beforeStatus != null and beforeAt != null and beforeId != null'>" +
            "and (m.status &gt; #{beforeStatus} or (m.status = #{beforeStatus} and (m.updated_at &lt; #{beforeAt} or (m.updated_at = #{beforeAt} and m.id &lt; #{beforeId})))) " +
            "</if>" +
            "order by m.status asc, m.updated_at desc, m.id desc limit #{limit}" +
            "</script>")
    List<MemoVO> list(@Param("coupleId") Long coupleId,
                      @Param("categoryId") Long categoryId,
                      @Param("status") Integer status,
                      @Param("q") String q,
                      @Param("beforeStatus") Integer beforeStatus,
                      @Param("beforeAt") Date beforeAt,
                      @Param("beforeId") Long beforeId,
                      @Param("limit") int limit);

    @Select("select m.id, m.couple_id as coupleId, m.category_id as categoryId, c.name as categoryName, m.title, m.content, m.status, " +
            "m.created_by as createdBy, uc.nickname as createdByNickname, m.updated_by as updatedBy, uu.nickname as updatedByNickname, m.created_at as createdAt, m.updated_at as updatedAt " +
            "from t_memo m " +
            "left join t_memo_category c on c.id = m.category_id " +
            "left join t_user uc on uc.id = m.created_by " +
            "left join t_user uu on uu.id = m.updated_by " +
            "where m.couple_id = #{coupleId} and m.id = #{id} limit 1")
    MemoVO findVoById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select id, couple_id as coupleId, category_id as categoryId, title, content, status, created_by as createdBy, updated_by as updatedBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_memo where couple_id = #{coupleId} and id = #{id} limit 1")
    Memo findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Insert("insert into t_memo(couple_id, category_id, title, content, status, created_by, updated_by, created_at, updated_at) " +
            "values(#{coupleId}, #{categoryId}, #{title}, #{content}, #{status}, #{createdBy}, #{updatedBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Memo m);

    @Update("update t_memo set category_id = #{categoryId}, title = #{title}, content = #{content}, status = #{status}, updated_by = #{updatedBy}, updated_at = #{updatedAt} " +
            "where couple_id = #{coupleId} and id = #{id}")
    int update(Memo m);

    @Update("update t_memo set status = #{status}, updated_by = #{updatedBy}, updated_at = #{updatedAt} where couple_id = #{coupleId} and id = #{id}")
    int updateStatus(@Param("coupleId") Long coupleId,
                     @Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updatedBy") Long updatedBy,
                     @Param("updatedAt") Date updatedAt);

    @Delete("delete from t_memo where couple_id = #{coupleId} and id = #{id}")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select count(1) from t_memo where couple_id = #{coupleId} and category_id = #{categoryId}")
    long countByCategoryId(@Param("coupleId") Long coupleId, @Param("categoryId") Long categoryId);
}
