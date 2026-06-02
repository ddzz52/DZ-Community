package com.dz.couple.module.timeline.mapper;

import com.dz.couple.module.timeline.dto.TimelineItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface TimelineMapper {

    @Select("<script>" +
            "select t.type as type, t.typeRank as typeRank, t.refId as refId, t.eventAt as eventAt, t.title as title, t.content as content, t.thumbUrl as thumbUrl " +
            "from (" +
            "<trim prefixOverrides='UNION ALL'>" +
            "<if test='types == null or types.size == 0 or types.contains(\"DIARY\")'>" +
            "UNION ALL" +
            " select 'DIARY' as type, 0 as typeRank, d.id as refId, d.created_at as eventAt, '日记' as title, " +
            "left(replace(replace(replace(d.content, '\\r', ' '), '\\n', ' '), '\\t', ' '), 80) as content, null as thumbUrl " +
            "from t_diary d where d.couple_id = #{coupleId} and (d.private_flag = 0 or d.author_id = #{userId}) " +
            "</if>" +
            "<if test='types == null or types.size == 0 or types.contains(\"PHOTO\")'>" +
            "UNION ALL" +
            " select 'PHOTO' as type, 1 as typeRank, p.id as refId, coalesce(p.shot_at, p.created_at) as eventAt, '照片' as title, " +
            "coalesce(p.title, '') as content, coalesce(p.thumb_url, p.url) as thumbUrl " +
            "from t_photo p where p.couple_id = #{coupleId} and p.deleted_flag = 0 " +
            "</if>" +
            "<if test='types == null or types.size == 0 or types.contains(\"ANNIVERSARY\")'>" +
            "UNION ALL" +
            " select 'ANNIVERSARY' as type, 2 as typeRank, a.id as refId, cast(concat(a.anniversary_date, ' 00:00:00') as datetime) as eventAt, " +
            "coalesce(a.title, '纪念日') as title, coalesce(a.type, '') as content, a.cover_thumb_url as thumbUrl " +
            "from t_anniversary a where a.couple_id = #{coupleId} " +
            "</if>" +
            "<if test='types == null or types.size == 0 or types.contains(\"WISH_DONE\")'>" +
            "UNION ALL" +
            " select 'WISH_DONE' as type, 3 as typeRank, w.id as refId, w.completed_at as eventAt, '完成心愿' as title, " +
            "w.content as content, null as thumbUrl " +
            "from t_wish_item w where w.couple_id = #{coupleId} and w.status = 1 and w.completed_at is not null " +
            "</if>" +
            "<if test='types == null or types.size == 0 or types.contains(\"SCRATCH_DONE\")'>" +
            "UNION ALL" +
            " select 'SCRATCH_DONE' as type, 4 as typeRank, s.id as refId, s.scratched_at as eventAt, '刮开刮刮卡' as title, " +
            "coalesce(s.content, '') as content, null as thumbUrl " +
            "from t_wish_scratch_card s where s.couple_id = #{coupleId} and s.status = 1 and s.scratched_at is not null " +
            "</if>" +
            "</trim>" +
            ") t " +
            "where 1 = 1 " +
            "<if test='fromAt != null'> and t.eventAt &gt;= #{fromAt} </if>" +
            "<if test='toAt != null'> and t.eventAt &lt;= #{toAt} </if>" +
            "<if test='beforeAt != null'> and (t.eventAt &lt; #{beforeAt} or (t.eventAt = #{beforeAt} and (t.typeRank &gt; #{beforeTypeRank} or (t.typeRank = #{beforeTypeRank} and t.refId &lt; #{beforeId})))) </if>" +
            "order by t.eventAt desc, t.typeRank asc, t.refId desc " +
            "limit #{limit}" +
            "</script>")
    List<TimelineItemVO> list(@Param("userId") Long userId,
                              @Param("coupleId") Long coupleId,
                              @Param("types") List<String> types,
                              @Param("fromAt") Date fromAt,
                              @Param("toAt") Date toAt,
                              @Param("beforeAt") Date beforeAt,
                              @Param("beforeTypeRank") Integer beforeTypeRank,
                              @Param("beforeId") Long beforeId,
                              @Param("limit") int limit);
}
