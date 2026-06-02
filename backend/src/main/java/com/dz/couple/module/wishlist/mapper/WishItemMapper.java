package com.dz.couple.module.wishlist.mapper;

import com.dz.couple.module.wishlist.dto.WishItemVO;
import com.dz.couple.module.wishlist.entity.WishItem;
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
public interface WishItemMapper {

    @Insert("insert into t_wish_item(couple_id, content, expected_at, priority, remark, status, created_by, created_at, updated_by, updated_at, completed_at, canceled_at, source_type, source_id) " +
            "values(#{coupleId}, #{content}, #{expectedAt}, #{priority}, #{remark}, #{status}, #{createdBy}, #{createdAt}, #{updatedBy}, #{updatedAt}, #{completedAt}, #{canceledAt}, #{sourceType}, #{sourceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WishItem it);

    @Select("<script>" +
            "select w.id as id, w.status as status, w.content as content, w.expected_at as expectedAt, w.priority as priority, w.remark as remark, " +
            "w.completed_at as completedAt, w.canceled_at as canceledAt, w.source_type as sourceType, w.source_id as sourceId, " +
            "w.created_by as createdBy, u1.nickname as createdByNickname, w.created_at as createdAt, " +
            "w.updated_by as updatedBy, u2.nickname as updatedByNickname, w.updated_at as updatedAt " +
            "from t_wish_item w " +
            "left join t_user u1 on u1.id = w.created_by " +
            "left join t_user u2 on u2.id = w.updated_by " +
            "where w.couple_id = #{coupleId} " +
            "<if test='status != null'> and w.status = #{status} </if>" +
            "order by w.status asc, w.priority asc, w.expected_at asc, w.id desc" +
            "</script>")
    List<WishItemVO> list(@Param("coupleId") Long coupleId, @Param("status") Integer status);

    @Select("select w.id as id, w.status as status, w.content as content, w.expected_at as expectedAt, w.priority as priority, w.remark as remark, " +
            "w.completed_at as completedAt, w.canceled_at as canceledAt, w.source_type as sourceType, w.source_id as sourceId, " +
            "w.created_by as createdBy, u1.nickname as createdByNickname, w.created_at as createdAt, " +
            "w.updated_by as updatedBy, u2.nickname as updatedByNickname, w.updated_at as updatedAt " +
            "from t_wish_item w " +
            "left join t_user u1 on u1.id = w.created_by " +
            "left join t_user u2 on u2.id = w.updated_by " +
            "where w.couple_id = #{coupleId} and w.status = 0 order by rand() limit 1")
    WishItemVO pickRandomPending(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, content, expected_at as expectedAt, priority, remark, status, created_by as createdBy, created_at as createdAt, updated_by as updatedBy, updated_at as updatedAt, completed_at as completedAt, canceled_at as canceledAt, source_type as sourceType, source_id as sourceId " +
            "from t_wish_item where couple_id = #{coupleId} and id = #{id} limit 1")
    WishItem findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select w.id as id, w.status as status, w.content as content, w.expected_at as expectedAt, w.priority as priority, w.remark as remark, " +
            "w.completed_at as completedAt, w.canceled_at as canceledAt, w.source_type as sourceType, w.source_id as sourceId, " +
            "w.created_by as createdBy, u1.nickname as createdByNickname, w.created_at as createdAt, " +
            "w.updated_by as updatedBy, u2.nickname as updatedByNickname, w.updated_at as updatedAt " +
            "from t_wish_item w " +
            "left join t_user u1 on u1.id = w.created_by " +
            "left join t_user u2 on u2.id = w.updated_by " +
            "where w.couple_id = #{coupleId} and w.id = #{id} limit 1")
    WishItemVO findVoById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Update("update t_wish_item set content = #{content}, expected_at = #{expectedAt}, priority = #{priority}, remark = #{remark}, updated_by = #{updatedBy}, updated_at = #{updatedAt} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int updateWhenPending(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("content") String content, @Param("expectedAt") String expectedAt,
                          @Param("priority") Integer priority, @Param("remark") String remark, @Param("updatedBy") Long updatedBy, @Param("updatedAt") Date updatedAt);

    @Update("update t_wish_item set status = 1, completed_at = #{at}, updated_by = #{userId}, updated_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int markCompleted(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Update("update t_wish_item set status = 2, canceled_at = #{at}, updated_by = #{userId}, updated_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int cancel(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Update("update t_wish_item set status = 0, canceled_at = null, updated_by = #{userId}, updated_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 2")
    int reopenFromCanceled(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Update("update t_wish_item set source_type = #{sourceType}, source_id = #{sourceId}, updated_by = #{userId}, updated_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int markScratchCard(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("sourceType") String sourceType, @Param("sourceId") Long sourceId,
                        @Param("userId") Long userId, @Param("at") Date at);

    @Delete("delete from t_wish_item where couple_id = #{coupleId} and id = #{id} and status = 0")
    int deleteWhenPending(@Param("coupleId") Long coupleId, @Param("id") Long id);
}
