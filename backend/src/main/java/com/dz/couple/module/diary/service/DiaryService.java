package com.dz.couple.module.diary.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.diary.dto.DiaryAttachmentVO;
import com.dz.couple.module.diary.dto.DiaryCommentCreateRequest;
import com.dz.couple.module.diary.dto.DiaryCommentVO;
import com.dz.couple.module.diary.dto.DiaryCreateRequest;
import com.dz.couple.module.diary.dto.DiaryUpdateRequest;
import com.dz.couple.module.diary.dto.DiaryVO;
import com.dz.couple.module.diary.dto.IdCount;
import com.dz.couple.module.diary.entity.Diary;
import com.dz.couple.module.diary.entity.DiaryAttachment;
import com.dz.couple.module.diary.entity.DiaryComment;
import com.dz.couple.module.diary.mapper.DiaryAttachmentMapper;
import com.dz.couple.module.diary.mapper.DiaryCommentMapper;
import com.dz.couple.module.diary.mapper.DiaryFavoriteMapper;
import com.dz.couple.module.diary.mapper.DiaryLikeMapper;
import com.dz.couple.module.diary.mapper.DiaryMapper;
import com.dz.couple.module.diary.mapper.DiaryPinMapper;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DiaryService {
    private final DiaryMapper diaryMapper;
    private final DiaryAttachmentMapper diaryAttachmentMapper;
    private final DiaryLikeMapper diaryLikeMapper;
    private final DiaryCommentMapper diaryCommentMapper;
    private final DiaryFavoriteMapper diaryFavoriteMapper;
    private final DiaryPinMapper diaryPinMapper;
    private final NotificationService notificationService;

    @Autowired
    public DiaryService(DiaryMapper diaryMapper,
                        DiaryAttachmentMapper diaryAttachmentMapper,
                        DiaryLikeMapper diaryLikeMapper,
                        DiaryCommentMapper diaryCommentMapper,
                        DiaryFavoriteMapper diaryFavoriteMapper,
                        DiaryPinMapper diaryPinMapper,
                        NotificationService notificationService) {
        this.diaryMapper = diaryMapper;
        this.diaryAttachmentMapper = diaryAttachmentMapper;
        this.diaryLikeMapper = diaryLikeMapper;
        this.diaryCommentMapper = diaryCommentMapper;
        this.diaryFavoriteMapper = diaryFavoriteMapper;
        this.diaryPinMapper = diaryPinMapper;
        this.notificationService = notificationService;
    }

    public List<DiaryVO> list(Long userId, Long coupleId, String keyword, int limit) {
        int l = Math.max(1, Math.min(limit, 100));
        List<Diary> diaries = diaryMapper.list(coupleId, userId, keyword, l);
        List<Long> ids = new ArrayList<>();
        for (Diary d : diaries) {
            ids.add(d.getId());
        }

        Map<Long, List<DiaryAttachmentVO>> imagesMap = new HashMap<>();
        Map<Long, Integer> likeMap = new HashMap<>();
        Map<Long, Integer> commentMap = new HashMap<>();
        Set<Long> likedSet = new HashSet<>();
        Set<Long> favoritedSet = new HashSet<>();
        Set<Long> pinnedSet = new HashSet<>();

        if (!ids.isEmpty()) {
            List<DiaryAttachment> atts = diaryAttachmentMapper.listByDiaryIds(coupleId, ids);
            if (atts != null) {
                for (DiaryAttachment a : atts) {
                    DiaryAttachmentVO vo = new DiaryAttachmentVO();
                    vo.setUrl(a.getUrl());
                    vo.setThumbUrl(a.getThumbUrl());
                    imagesMap.computeIfAbsent(a.getDiaryId(), k -> new ArrayList<>()).add(vo);
                }
            }
            List<IdCount> likes = diaryLikeMapper.countByDiaryIds(coupleId, ids);
            if (likes != null) {
                for (IdCount c : likes) {
                    likeMap.put(c.getId(), c.getCount() == null ? 0 : c.getCount());
                }
            }
            List<IdCount> comments = diaryCommentMapper.countByDiaryIds(coupleId, ids);
            if (comments != null) {
                for (IdCount c : comments) {
                    commentMap.put(c.getId(), c.getCount() == null ? 0 : c.getCount());
                }
            }
            List<Long> liked = diaryLikeMapper.listLikedDiaryIds(coupleId, userId, ids);
            if (liked != null) {
                likedSet.addAll(liked);
            }
            List<Long> favorited = diaryFavoriteMapper.listFavoritedDiaryIds(coupleId, userId, ids);
            if (favorited != null) {
                favoritedSet.addAll(favorited);
            }
            List<Long> pinned = diaryPinMapper.listPinnedDiaryIds(coupleId, userId, ids);
            if (pinned != null) {
                pinnedSet.addAll(pinned);
            }
        }

        if (diaries != null && diaries.size() > 1 && !pinnedSet.isEmpty()) {
            Collections.sort(diaries, new Comparator<Diary>() {
                @Override
                public int compare(Diary a, Diary b) {
                    boolean ap = a != null && a.getId() != null && pinnedSet.contains(a.getId());
                    boolean bp = b != null && b.getId() != null && pinnedSet.contains(b.getId());
                    if (ap != bp) {
                        return ap ? -1 : 1;
                    }
                    Date ad = a == null ? null : a.getCreatedAt();
                    Date bd = b == null ? null : b.getCreatedAt();
                    long at = ad == null ? 0L : ad.getTime();
                    long bt = bd == null ? 0L : bd.getTime();
                    return Long.compare(bt, at);
                }
            });
        }

        List<DiaryVO> out = new ArrayList<>();
        for (Diary d : diaries) {
            DiaryVO vo = toVO(d);
            vo.setImages(imagesMap.getOrDefault(d.getId(), new ArrayList<>()));
            vo.setLikeCount(likeMap.getOrDefault(d.getId(), 0));
            vo.setCommentCount(commentMap.getOrDefault(d.getId(), 0));
            vo.setLiked(likedSet.contains(d.getId()));
            vo.setFavorited(favoritedSet.contains(d.getId()));
            vo.setPinned(pinnedSet.contains(d.getId()));
            out.add(vo);
        }
        return out;
    }

    public DiaryVO create(Long userId, Long coupleId, DiaryCreateRequest req) {
        List<String> images = req.getImages();
        if (images != null && images.size() > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配图最多5张");
        }
        Diary d = new Diary();
        d.setCoupleId(coupleId);
        d.setAuthorId(userId);
        d.setContent(req.getContent());
        d.setMood(safeTrim(req.getMood()));
        d.setPrivateFlag(req.getPrivateFlag() != null && req.getPrivateFlag() ? 1 : 0);
        Date now = new Date();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        diaryMapper.insert(d);

        if (images != null) {
            int i = 0;
            for (String url : images) {
                String u = safeTrim(url);
                if (u == null) {
                    continue;
                }
                DiaryAttachment a = new DiaryAttachment();
                a.setDiaryId(d.getId());
                a.setCoupleId(coupleId);
                a.setUrl(u);
                a.setThumbUrl(null);
                a.setSortNo(i++);
                a.setCreatedAt(now);
                diaryAttachmentMapper.insert(a);
            }
        }
        DiaryVO vo = toVO(d);
        vo.setImages(loadImages(coupleId, d.getId()));
        vo.setLikeCount(0);
        vo.setCommentCount(0);
        vo.setLiked(false);
        vo.setFavorited(false);
        vo.setPinned(false);
        return vo;
    }

    public DiaryVO update(Long userId, Long coupleId, Long id, DiaryUpdateRequest req) {
        Diary exist = diaryMapper.findById(coupleId, id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (exist.getAuthorId() == null || !exist.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        List<String> images = req.getImages();
        if (images != null && images.size() > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配图最多5张");
        }

        exist.setContent(req.getContent());
        exist.setMood(safeTrim(req.getMood()));
        exist.setPrivateFlag(req.getPrivateFlag() != null && req.getPrivateFlag() ? 1 : 0);
        exist.setUpdatedAt(new Date());
        int n = diaryMapper.updateByAuthor(exist);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        diaryAttachmentMapper.deleteByDiaryId(coupleId, id);
        if (images != null) {
            int i = 0;
            Date now = new Date();
            for (String url : images) {
                String u = safeTrim(url);
                if (u == null) {
                    continue;
                }
                DiaryAttachment a = new DiaryAttachment();
                a.setDiaryId(id);
                a.setCoupleId(coupleId);
                a.setUrl(u);
                a.setThumbUrl(null);
                a.setSortNo(i++);
                a.setCreatedAt(now);
                diaryAttachmentMapper.insert(a);
            }
        }

        DiaryVO vo = toVO(exist);
        vo.setImages(loadImages(coupleId, id));
        vo.setLikeCount(countLikes(coupleId, id));
        vo.setCommentCount(countComments(coupleId, id));
        vo.setLiked(diaryLikeMapper.exists(coupleId, id, userId) > 0);
        vo.setFavorited(diaryFavoriteMapper.exists(coupleId, id, userId) > 0);
        vo.setPinned(diaryPinMapper.exists(coupleId, id, userId) > 0);
        return vo;
    }

    public void delete(Long userId, Long coupleId, Long id) {
        Diary exist = diaryMapper.findById(coupleId, id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (exist.getAuthorId() == null || !exist.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        diaryAttachmentMapper.deleteByDiaryId(coupleId, id);
        int n = diaryMapper.deleteByAuthor(coupleId, id, userId);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public DiaryVO toggleLike(Long userId, Long coupleId, Long diaryId) {
        Diary d = diaryMapper.findById(coupleId, diaryId);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (d.getPrivateFlag() != null && d.getPrivateFlag() == 1 && (d.getAuthorId() == null || !d.getAuthorId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int exists = diaryLikeMapper.exists(coupleId, diaryId, userId);
        if (exists > 0) {
            diaryLikeMapper.delete(coupleId, diaryId, userId);
        } else {
            diaryLikeMapper.insert(coupleId, diaryId, userId, new Date());
        }
        DiaryVO vo = toVO(d);
        vo.setImages(loadImages(coupleId, diaryId));
        vo.setLikeCount(countLikes(coupleId, diaryId));
        vo.setCommentCount(countComments(coupleId, diaryId));
        vo.setLiked(exists <= 0);
        vo.setFavorited(diaryFavoriteMapper.exists(coupleId, diaryId, userId) > 0);
        vo.setPinned(diaryPinMapper.exists(coupleId, diaryId, userId) > 0);
        return vo;
    }

    public DiaryVO toggleFavorite(Long userId, Long coupleId, Long diaryId) {
        Diary d = diaryMapper.findById(coupleId, diaryId);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (d.getPrivateFlag() != null && d.getPrivateFlag() == 1 && (d.getAuthorId() == null || !d.getAuthorId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int exists = diaryFavoriteMapper.exists(coupleId, diaryId, userId);
        if (exists > 0) {
            diaryFavoriteMapper.delete(coupleId, diaryId, userId);
        } else {
            diaryFavoriteMapper.insert(coupleId, diaryId, userId, new Date());
        }
        DiaryVO vo = toVO(d);
        vo.setImages(loadImages(coupleId, diaryId));
        vo.setLikeCount(countLikes(coupleId, diaryId));
        vo.setCommentCount(countComments(coupleId, diaryId));
        vo.setLiked(diaryLikeMapper.exists(coupleId, diaryId, userId) > 0);
        vo.setFavorited(exists <= 0);
        vo.setPinned(diaryPinMapper.exists(coupleId, diaryId, userId) > 0);
        return vo;
    }

    public DiaryVO togglePin(Long userId, Long coupleId, Long diaryId) {
        Diary d = diaryMapper.findById(coupleId, diaryId);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (d.getPrivateFlag() != null && d.getPrivateFlag() == 1 && (d.getAuthorId() == null || !d.getAuthorId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int exists = diaryPinMapper.exists(coupleId, diaryId, userId);
        if (exists > 0) {
            diaryPinMapper.delete(coupleId, diaryId, userId);
        } else {
            diaryPinMapper.insert(coupleId, diaryId, userId, new Date());
        }
        DiaryVO vo = toVO(d);
        vo.setImages(loadImages(coupleId, diaryId));
        vo.setLikeCount(countLikes(coupleId, diaryId));
        vo.setCommentCount(countComments(coupleId, diaryId));
        vo.setLiked(diaryLikeMapper.exists(coupleId, diaryId, userId) > 0);
        vo.setFavorited(diaryFavoriteMapper.exists(coupleId, diaryId, userId) > 0);
        vo.setPinned(exists <= 0);
        return vo;
    }

    public List<DiaryCommentVO> listComments(Long userId, Long coupleId, Long diaryId, int limit) {
        Diary d = diaryMapper.findById(coupleId, diaryId);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (d.getPrivateFlag() != null && d.getPrivateFlag() == 1 && (d.getAuthorId() == null || !d.getAuthorId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int l = Math.max(1, Math.min(limit, 200));
        List<DiaryComment> list = diaryCommentMapper.listByDiaryId(coupleId, diaryId, l);
        List<DiaryCommentVO> out = new ArrayList<>();
        if (list != null) {
            for (DiaryComment c : list) {
                DiaryCommentVO vo = new DiaryCommentVO();
                vo.setId(c.getId());
                vo.setAuthorId(c.getAuthorId());
                vo.setContent(c.getContent());
                vo.setCreatedAt(c.getCreatedAt());
                out.add(vo);
            }
        }
        return out;
    }

    public DiaryCommentVO addComment(Long userId, Long coupleId, Long diaryId, DiaryCommentCreateRequest req) {
        Diary d = diaryMapper.findById(coupleId, diaryId);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (d.getPrivateFlag() != null && d.getPrivateFlag() == 1 && (d.getAuthorId() == null || !d.getAuthorId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        DiaryComment c = new DiaryComment();
        c.setDiaryId(diaryId);
        c.setCoupleId(coupleId);
        c.setAuthorId(userId);
        c.setContent(req.getContent());
        c.setCreatedAt(new Date());
        diaryCommentMapper.insert(c);
        if (d.getAuthorId() != null && !d.getAuthorId().equals(userId)) {
            Date today = Date.from(java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            notificationService.create(
                    d.getAuthorId(),
                    coupleId,
                    NotificationTypes.DIARY_COMMENT,
                    "日记新评论",
                    "对方评论了你的日记：" + preview(req.getContent()),
                    c.getId(),
                    today,
                    false
            );
        }
        DiaryCommentVO vo = new DiaryCommentVO();
        vo.setId(c.getId());
        vo.setAuthorId(c.getAuthorId());
        vo.setContent(c.getContent());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    public void deleteComment(Long userId, Long coupleId, Long commentId) {
        int n = diaryCommentMapper.deleteByAuthor(coupleId, commentId, userId);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private List<DiaryAttachmentVO> loadImages(Long coupleId, Long diaryId) {
        List<DiaryAttachment> list = diaryAttachmentMapper.listByDiaryId(coupleId, diaryId);
        List<DiaryAttachmentVO> out = new ArrayList<>();
        if (list != null) {
            for (DiaryAttachment a : list) {
                DiaryAttachmentVO vo = new DiaryAttachmentVO();
                vo.setUrl(a.getUrl());
                vo.setThumbUrl(a.getThumbUrl());
                out.add(vo);
            }
        }
        return out;
    }

    private int countLikes(Long coupleId, Long diaryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(diaryId);
        List<IdCount> res = diaryLikeMapper.countByDiaryIds(coupleId, ids);
        if (res == null || res.isEmpty() || res.get(0).getCount() == null) {
            return 0;
        }
        return res.get(0).getCount();
    }

    private int countComments(Long coupleId, Long diaryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(diaryId);
        List<IdCount> res = diaryCommentMapper.countByDiaryIds(coupleId, ids);
        if (res == null || res.isEmpty() || res.get(0).getCount() == null) {
            return 0;
        }
        return res.get(0).getCount();
    }

    private DiaryVO toVO(Diary d) {
        DiaryVO vo = new DiaryVO();
        vo.setId(d.getId());
        vo.setAuthorId(d.getAuthorId());
        vo.setContent(d.getContent());
        vo.setContentPreview(preview(d.getContent()));
        vo.setMood(d.getMood());
        vo.setPrivateFlag(d.getPrivateFlag() != null && d.getPrivateFlag() == 1);
        vo.setCreatedAt(d.getCreatedAt());
        vo.setUpdatedAt(d.getUpdatedAt());
        return vo;
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String c = content.trim().replaceAll("\\s+", " ");
        if (c.length() <= 80) {
            return c;
        }
        return c.substring(0, 80) + "…";
    }

    private String safeTrim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
