package com.dz.couple.module.photo.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.config.AppProperties;
import com.dz.couple.module.album.entity.Album;
import com.dz.couple.module.album.service.AlbumService;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.photo.dto.PhotoCreateRequest;
import com.dz.couple.module.photo.dto.PhotoUpdateRequest;
import com.dz.couple.module.photo.dto.PhotoVO;
import com.dz.couple.module.photo.dto.IdCount;
import com.dz.couple.module.photo.entity.Photo;
import com.dz.couple.module.photo.entity.PhotoComment;
import com.dz.couple.module.photo.dto.PhotoCommentCreateRequest;
import com.dz.couple.module.photo.dto.PhotoCommentVO;
import com.dz.couple.module.photo.mapper.PhotoMapper;
import com.dz.couple.module.photo.mapper.PhotoLikeMapper;
import com.dz.couple.module.photo.mapper.PhotoCommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PhotoService {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PhotoMapper photoMapper;
    private final AlbumService albumService;
    private final PhotoLikeMapper photoLikeMapper;
    private final PhotoCommentMapper photoCommentMapper;
    private final AppProperties appProperties;
    private final NotificationService notificationService;

    @Autowired
    public PhotoService(PhotoMapper photoMapper, AlbumService albumService, PhotoLikeMapper photoLikeMapper, PhotoCommentMapper photoCommentMapper, AppProperties appProperties, NotificationService notificationService) {
        this.photoMapper = photoMapper;
        this.albumService = albumService;
        this.photoLikeMapper = photoLikeMapper;
        this.photoCommentMapper = photoCommentMapper;
        this.appProperties = appProperties;
        this.notificationService = notificationService;
    }

    public List<PhotoVO> list(Long userId, Long coupleId, Long albumId, Date from, Date to, boolean deletedOnly, Boolean orderAsc, int limit) {
        int l = Math.max(1, Math.min(limit, 200));
        List<Photo> list = photoMapper.list(coupleId, albumId, from, to, deletedOnly, orderAsc, l);
        List<Long> ids = new ArrayList<>();
        for (Photo p : list) {
            if (p != null && p.getId() != null) {
                ids.add(p.getId());
            }
        }
        Set<Long> likedSet = new HashSet<>();
        Map<Long, Integer> likeCountMap = new HashMap<>();
        Map<Long, Integer> commentCountMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<Long> likedIds = photoLikeMapper.listLikedPhotoIds(userId, coupleId, ids);
            if (likedIds != null) {
                likedSet.addAll(likedIds);
            }
            List<IdCount> lc = photoLikeMapper.countByPhotoIds(ids);
            if (lc != null) {
                for (IdCount x : lc) {
                    likeCountMap.put(x.getId(), x.getCount() == null ? 0 : x.getCount());
                }
            }
            List<IdCount> cc = photoCommentMapper.countByPhotoIds(ids);
            if (cc != null) {
                for (IdCount x : cc) {
                    commentCountMap.put(x.getId(), x.getCount() == null ? 0 : x.getCount());
                }
            }
        }
        List<PhotoVO> out = new ArrayList<>();
        for (Photo p : list) {
            out.add(toVO(p, likedSet, likeCountMap, commentCountMap));
        }
        return out;
    }

    public List<PhotoVO> createBatch(Long userId, Long coupleId, List<PhotoCreateRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
        }
        if (reqs.size() > 10) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一次最多10张");
        }
        albumService.ensureDefaultAlbum(coupleId);
        Date now = new Date();
        List<PhotoVO> out = new ArrayList<>();
        for (PhotoCreateRequest r : reqs) {
            Photo p = new Photo();
            p.setCoupleId(coupleId);
            p.setUploaderId(userId);
            p.setAlbumId(r.getAlbumId());
            p.setUrl(safeTrim(r.getUrl()));
            p.setThumbUrl(safeTrim(r.getThumbUrl()));
            p.setTitle(null);
            p.setLocation(null);
            p.setMood(null);
            p.setTags(null);
            p.setCoverFlag(0);
            p.setCoverSetAt(null);
            p.setShotAt(parseDateTime(r.getShotAt()));
            p.setCreatedAt(now);
            photoMapper.insert(p);
            out.add(toVO(p, null, null, null));
        }
        return out;
    }

    public PhotoVO update(Long userId, Long coupleId, Long id, PhotoUpdateRequest req) {
        Photo p = photoMapper.findById(coupleId, id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (p.getDeletedFlag() != null && p.getDeletedFlag() == 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "回收站照片不可编辑");
        }
        Long targetAlbumId = p.getAlbumId();
        if (req.getAlbumId() != null) {
            Album target = albumService.getById(coupleId, req.getAlbumId());
            if ("最近删除".equals(target.getName())) {
                throw new BusinessException(ErrorCode.CONFLICT, "请使用删除操作移入回收站");
            }
            targetAlbumId = req.getAlbumId();
            p.setAlbumId(targetAlbumId);
        }
        p.setTitle(safeTrim(req.getTitle()));
        p.setLocation(safeTrim(req.getLocation()));
        p.setMood(safeTrim(req.getMood()));
        p.setTags(safeTrim(req.getTags()));
        if (req.getShotAt() != null) {
            p.setShotAt(parseDateTime(req.getShotAt()));
        }
        int n = photoMapper.update(p);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        if (req.getAlbumId() != null) {
            StorageMoveResult moved = moveStorageIfPossible(coupleId, p.getUrl(), p.getThumbUrl(), targetAlbumId);
            if (moved != null && (moved.urlChanged || moved.thumbChanged)) {
                photoMapper.updateUrls(coupleId, id, moved.newUrl, moved.newThumbUrl);
                p.setUrl(moved.newUrl);
                p.setThumbUrl(moved.newThumbUrl);
            }
        }
        return enrichOne(userId, coupleId, p);
    }

    public PhotoVO toggleLike(Long userId, Long coupleId, Long photoId) {
        Photo p = photoMapper.findById(coupleId, photoId);
        if (p == null || (p.getDeletedFlag() != null && p.getDeletedFlag() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Integer ex = photoLikeMapper.exists(photoId, userId);
        if (ex != null && ex == 1) {
            photoLikeMapper.delete(photoId, userId);
        } else {
            photoLikeMapper.insert(photoId, userId, coupleId, new Date());
            if (p.getUploaderId() != null && !p.getUploaderId().equals(userId)) {
                Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
                notificationService.create(
                        p.getUploaderId(),
                        coupleId,
                        NotificationTypes.PHOTO_LIKE,
                        "照片被点赞",
                        "对方点赞了你的照片。",
                        photoId,
                        today,
                        true
                );
            }
        }
        return enrichOne(userId, coupleId, p);
    }

    public List<PhotoCommentVO> listComments(Long coupleId, Long photoId, int limit) {
        Photo p = photoMapper.findById(coupleId, photoId);
        if (p == null || (p.getDeletedFlag() != null && p.getDeletedFlag() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        int l = Math.max(1, Math.min(limit, 200));
        List<PhotoComment> list = photoCommentMapper.listByPhotoId(photoId, l);
        List<PhotoCommentVO> out = new ArrayList<>();
        if (list != null) {
            for (PhotoComment c : list) {
                PhotoCommentVO vo = new PhotoCommentVO();
                vo.setId(c.getId());
                vo.setAuthorId(c.getAuthorId());
                vo.setContent(c.getContent());
                vo.setCreatedAt(c.getCreatedAt());
                out.add(vo);
            }
        }
        return out;
    }

    public PhotoCommentVO addComment(Long userId, Long coupleId, Long photoId, PhotoCommentCreateRequest req) {
        Photo p = photoMapper.findById(coupleId, photoId);
        if (p == null || (p.getDeletedFlag() != null && p.getDeletedFlag() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        PhotoComment c = new PhotoComment();
        c.setPhotoId(photoId);
        c.setCoupleId(coupleId);
        c.setAuthorId(userId);
        c.setContent(safeTrim(req.getContent()));
        c.setCreatedAt(new Date());
        photoCommentMapper.insert(c);
        PhotoCommentVO vo = new PhotoCommentVO();
        vo.setId(c.getId());
        vo.setAuthorId(c.getAuthorId());
        vo.setContent(c.getContent());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    public void deleteComment(Long userId, Long coupleId, Long commentId) {
        PhotoComment c = photoCommentMapper.findById(commentId);
        if (c == null || c.getCoupleId() == null || !c.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (c.getAuthorId() == null || !c.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        photoCommentMapper.delete(commentId);
    }

    public PhotoVO toggleCover(Long coupleId, Long id) {
        Photo p = photoMapper.findById(coupleId, id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (p.getDeletedFlag() != null && p.getDeletedFlag() == 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "回收站照片不可设为封面");
        }
        if (p.getCoverFlag() != null && p.getCoverFlag() == 1) {
            photoMapper.unsetCover(coupleId, id);
            p.setCoverFlag(0);
            p.setCoverSetAt(null);
            return toVO(p, null, null, null);
        }
        photoMapper.clearCover(coupleId);
        Date now = new Date();
        photoMapper.setCover(coupleId, id, now);
        p.setCoverFlag(1);
        p.setCoverSetAt(now);
        return toVO(p, null, null, null);
    }

    public void delete(Long coupleId, Long id) {
        Photo p = photoMapper.findById(coupleId, id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Long trashId = albumService.ensureTrashAlbum(coupleId);
        StorageMoveResult moved = moveStorageIfPossible(coupleId, p.getUrl(), p.getThumbUrl(), trashId);
        int n = photoMapper.softDelete(coupleId, id, trashId, new Date());
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        if (moved != null && (moved.urlChanged || moved.thumbChanged)) {
            photoMapper.updateUrls(coupleId, id, moved.newUrl, moved.newThumbUrl);
        }
    }

    public void restore(Long coupleId, Long id) {
        Photo p = photoMapper.findById(coupleId, id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (p.getDeletedFlag() == null || p.getDeletedFlag() != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "该照片未在回收站");
        }
        Long toAlbumId = p.getDeletedFromAlbumId();
        if (toAlbumId != null) {
            Album target = albumService.getById(coupleId, toAlbumId);
            if ("最近删除".equals(target.getName())) {
                toAlbumId = null;
            }
        }
        int n = photoMapper.restore(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        StorageMoveResult moved = moveStorageIfPossible(coupleId, p.getUrl(), p.getThumbUrl(), toAlbumId);
        if (moved != null && (moved.urlChanged || moved.thumbChanged)) {
            photoMapper.updateUrls(coupleId, id, moved.newUrl, moved.newThumbUrl);
        }
    }

    public void purge(Long coupleId, Long id) {
        Photo p = photoMapper.findById(coupleId, id);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        deleteFileQuietly(resolveUploadPath(p.getUrl()));
        deleteFileQuietly(resolveUploadPath(p.getThumbUrl()));
        int n = photoMapper.delete(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public int deleteBatch(Long coupleId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            delete(coupleId, id);
            n++;
        }
        return n;
    }

    public int moveBatch(Long coupleId, List<Long> ids, Long albumId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        if (albumId != null) {
            Album target = albumService.getById(coupleId, albumId);
            if ("最近删除".equals(target.getName())) {
                throw new BusinessException(ErrorCode.CONFLICT, "请使用删除操作移入回收站");
            }
            if ("全部照片".equals(target.getName())) {
                albumId = null;
            }
        }
        List<Photo> photos = photoMapper.listByIds(coupleId, ids);
        if (photos == null || photos.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Photo p : photos) {
            if (p == null || p.getId() == null) {
                continue;
            }
            if (p.getDeletedFlag() != null && p.getDeletedFlag() == 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "回收站照片不可移动");
            }
            StorageMoveResult moved = moveStorageIfPossible(coupleId, p.getUrl(), p.getThumbUrl(), albumId);
            String newUrl = moved == null ? p.getUrl() : moved.newUrl;
            String newThumb = moved == null ? p.getThumbUrl() : moved.newThumbUrl;
            photoMapper.updateStorage(coupleId, p.getId(), albumId, newUrl, newThumb);
            n++;
        }
        return n;
    }

    public void writeZip(Long coupleId, List<Long> ids, OutputStream out) {
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择照片");
        }
        List<Photo> photos = photoMapper.listByIds(coupleId, ids);
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "照片不存在");
        }
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            int idx = 0;
            for (Photo p : photos) {
                if (p == null) {
                    continue;
                }
                String url = p.getUrl();
                Path file = resolveUploadPath(url);
                if (file == null || !Files.exists(file)) {
                    continue;
                }
                String name = extractName(url);
                if (name == null) {
                    name = "photo_" + (++idx) + ".jpg";
                }
                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
            }
            zos.finish();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "打包下载失败");
        }
    }

    private StorageMoveResult moveStorageIfPossible(Long coupleId, String url, String thumbUrl, Long albumId) {
        if (coupleId == null) {
            return null;
        }
        StorageMoveResult r = new StorageMoveResult();
        r.newUrl = url;
        r.newThumbUrl = thumbUrl;
        if (url != null && url.startsWith("/uploads/")) {
            String day = extractDay(url);
            String name = extractName(url);
            String nextUrl = buildPhotoUrl(coupleId, albumId, day, name);
            if (nextUrl != null && !nextUrl.equals(url)) {
                if (moveFileQuietly(resolveUploadPath(url), resolveUploadPath(nextUrl))) {
                    r.newUrl = nextUrl;
                    r.urlChanged = true;
                }
            }
        }
        if (thumbUrl != null && thumbUrl.startsWith("/uploads/")) {
            String day = extractDay(thumbUrl);
            String name = extractName(thumbUrl);
            String nextUrl = buildPhotoUrl(coupleId, albumId, day, name);
            if (nextUrl != null && !nextUrl.equals(thumbUrl)) {
                if (moveFileQuietly(resolveUploadPath(thumbUrl), resolveUploadPath(nextUrl))) {
                    r.newThumbUrl = nextUrl;
                    r.thumbChanged = true;
                }
            }
        }
        if (!r.urlChanged && !r.thumbChanged) {
            return null;
        }
        return r;
    }

    private String buildPhotoUrl(Long coupleId, Long albumId, String day, String name) {
        if (coupleId == null || name == null || name.trim().isEmpty()) {
            return null;
        }
        String d = day == null ? LocalDate.now().format(DAY) : day;
        String albumKey = albumId == null ? "uncategorized" : String.valueOf(albumId);
        albumKey = safeSegment(albumKey);
        return "/uploads/photos/" + coupleId + "/" + albumKey + "/" + d + "/" + name;
    }

    private String extractName(String url) {
        if (url == null) return null;
        int idx = url.lastIndexOf('/');
        if (idx < 0 || idx >= url.length() - 1) return null;
        return url.substring(idx + 1);
    }

    private String extractDay(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p != null && p.matches("^\\d{8}$")) {
                return p;
            }
        }
        return null;
    }

    private Path resolveUploadPath(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return null;
        }
        String rel = url.substring("/uploads/".length());
        Path root = uploadRoot();
        Path p = root.resolve(rel).normalize();
        if (!p.startsWith(root)) {
            return null;
        }
        return p;
    }

    private Path uploadRoot() {
        String dir = appProperties.getUploadDir();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "uploads";
        }
        return Paths.get(dir).toAbsolutePath().normalize();
    }

    private boolean moveFileQuietly(Path from, Path to) {
        if (from == null || to == null) return false;
        try {
            if (!Files.exists(from)) {
                return false;
            }
            Files.createDirectories(to.getParent());
            Files.move(from, to);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteFileQuietly(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (Exception e) {
            return;
        }
    }

    private String safeSegment(String s) {
        String t = String.valueOf(s == null ? "" : s).trim();
        if (t.isEmpty()) {
            return "unknown";
        }
        String out = t.replaceAll("[^0-9A-Za-z_-]", "_");
        if (out.isEmpty()) {
            return "unknown";
        }
        return out;
    }

    private static class StorageMoveResult {
        private String newUrl;
        private String newThumbUrl;
        private boolean urlChanged;
        private boolean thumbChanged;
    }

    private PhotoVO toVO(Photo p, Set<Long> likedSet, Map<Long, Integer> likeCountMap, Map<Long, Integer> commentCountMap) {
        PhotoVO vo = new PhotoVO();
        vo.setId(p.getId());
        vo.setAlbumId(p.getAlbumId());
        vo.setUploaderId(p.getUploaderId());
        vo.setTitle(p.getTitle());
        vo.setLocation(p.getLocation());
        vo.setMood(p.getMood());
        vo.setTags(p.getTags());
        vo.setUrl(p.getUrl());
        vo.setThumbUrl(p.getThumbUrl());
        vo.setCover(p.getCoverFlag() != null && p.getCoverFlag() == 1);
        vo.setShotAt(p.getShotAt());
        vo.setCreatedAt(p.getCreatedAt());
        if (p.getId() != null) {
            Long id = p.getId();
            vo.setLiked(likedSet != null && likedSet.contains(id));
            vo.setLikeCount(likeCountMap != null && likeCountMap.containsKey(id) ? likeCountMap.get(id) : 0);
            vo.setCommentCount(commentCountMap != null && commentCountMap.containsKey(id) ? commentCountMap.get(id) : 0);
        } else {
            vo.setLiked(false);
            vo.setLikeCount(0);
            vo.setCommentCount(0);
        }
        return vo;
    }

    private PhotoVO enrichOne(Long userId, Long coupleId, Photo p) {
        Set<Long> likedSet = new HashSet<>();
        Map<Long, Integer> likeCountMap = new HashMap<>();
        Map<Long, Integer> commentCountMap = new HashMap<>();
        if (p != null && p.getId() != null) {
            Long id = p.getId();
            Integer ex = photoLikeMapper.exists(id, userId);
            if (ex != null && ex == 1) {
                likedSet.add(id);
            }
            likeCountMap.put(id, (int) photoLikeMapper.countByPhotoId(id));
            commentCountMap.put(id, (int) photoCommentMapper.countByPhotoId(id));
        }
        return toVO(p, likedSet, likeCountMap, commentCountMap);
    }

    private Date parseDateTime(String s) {
        String t = safeTrim(s);
        if (t == null) {
            return null;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(t, DTF);
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拍摄时间格式需为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
