package com.dz.couple.module.album.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.album.dto.AlbumCreateRequest;
import com.dz.couple.module.album.dto.AlbumUpdateRequest;
import com.dz.couple.module.album.dto.AlbumVO;
import com.dz.couple.module.album.entity.Album;
import com.dz.couple.module.album.mapper.AlbumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AlbumService {
    private final AlbumMapper albumMapper;
    private static final String ALBUM_ALL = "全部照片";
    private static final String ALBUM_TRASH = "最近删除";

    @Autowired
    public AlbumService(AlbumMapper albumMapper) {
        this.albumMapper = albumMapper;
    }

    public List<AlbumVO> list(Long coupleId) {
        ensureDefaultAlbum(coupleId);
        ensureTrashAlbum(coupleId);
        List<Album> list = albumMapper.listByCoupleId(coupleId);
        List<AlbumVO> out = new ArrayList<>();
        for (Album a : list) {
            AlbumVO vo = new AlbumVO();
            vo.setId(a.getId());
            vo.setName(a.getName());
            vo.setSortNo(a.getSortNo());
            vo.setDeletable(a.getDeletable() != null && a.getDeletable() == 1);
            int count;
            if (ALBUM_ALL.equals(a.getName())) {
                count = albumMapper.countAllPhotos(coupleId);
            } else if (ALBUM_TRASH.equals(a.getName())) {
                count = albumMapper.countDeletedPhotos(coupleId);
            } else {
                count = albumMapper.countPhotos(coupleId, a.getId());
            }
            vo.setPhotoCount(count);
            out.add(vo);
        }
        return out;
    }

    public AlbumVO create(Long coupleId, AlbumCreateRequest req) {
        ensureDefaultAlbum(coupleId);
        ensureTrashAlbum(coupleId);
        String name = safeName(req.getName());
        if (ALBUM_ALL.equals(name) || ALBUM_TRASH.equals(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类名不可用");
        }
        Album exist = albumMapper.findByName(coupleId, name);
        if (exist != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类名已存在");
        }
        if (albumMapper.countByCoupleId(coupleId) >= 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类数量过多");
        }
        Album a = new Album();
        a.setCoupleId(coupleId);
        a.setName(name);
        a.setSortNo(100);
        a.setDeletable(1);
        Date now = new Date();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        albumMapper.insert(a);
        AlbumVO vo = new AlbumVO();
        vo.setId(a.getId());
        vo.setName(a.getName());
        vo.setSortNo(a.getSortNo());
        vo.setDeletable(true);
        vo.setPhotoCount(0);
        return vo;
    }

    public AlbumVO update(Long coupleId, Long id, AlbumUpdateRequest req) {
        ensureDefaultAlbum(coupleId);
        ensureTrashAlbum(coupleId);
        Album a = albumMapper.findById(coupleId, id);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (a.getDeletable() == null || a.getDeletable() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String name = safeName(req.getName());
        if (ALBUM_ALL.equals(name) || ALBUM_TRASH.equals(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类名不可用");
        }
        Album byName = albumMapper.findByName(coupleId, name);
        if (byName != null && !byName.getId().equals(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类名已存在");
        }
        a.setName(name);
        if (req.getSortNo() != null) {
            a.setSortNo(req.getSortNo());
        }
        a.setUpdatedAt(new Date());
        int n = albumMapper.update(a);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        AlbumVO vo = new AlbumVO();
        vo.setId(a.getId());
        vo.setName(a.getName());
        vo.setSortNo(a.getSortNo());
        vo.setDeletable(true);
        vo.setPhotoCount(albumMapper.countPhotos(coupleId, id));
        return vo;
    }

    public void delete(Long coupleId, Long id) {
        ensureDefaultAlbum(coupleId);
        ensureTrashAlbum(coupleId);
        Album a = albumMapper.findById(coupleId, id);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (a.getDeletable() == null || a.getDeletable() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        albumMapper.detachPhotos(coupleId, id);
        int n = albumMapper.delete(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public Album getById(Long coupleId, Long id) {
        if (id == null) {
            return null;
        }
        ensureDefaultAlbum(coupleId);
        ensureTrashAlbum(coupleId);
        Album a = albumMapper.findById(coupleId, id);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return a;
    }

    public Long ensureDefaultAlbum(Long coupleId) {
        Album exist = albumMapper.findByName(coupleId, ALBUM_ALL);
        if (exist != null) {
            return exist.getId();
        }
        Album a = new Album();
        a.setCoupleId(coupleId);
        a.setName(ALBUM_ALL);
        a.setSortNo(0);
        a.setDeletable(0);
        Date now = new Date();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        albumMapper.insert(a);
        return a.getId();
    }

    public Long ensureTrashAlbum(Long coupleId) {
        Album exist = albumMapper.findByName(coupleId, ALBUM_TRASH);
        if (exist != null) {
            return exist.getId();
        }
        Album a = new Album();
        a.setCoupleId(coupleId);
        a.setName(ALBUM_TRASH);
        a.setSortNo(1);
        a.setDeletable(0);
        Date now = new Date();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        albumMapper.insert(a);
        return a.getId();
    }

    private String safeName(String s) {
        if (s == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入分类名");
        }
        String t = s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入分类名");
        }
        if (t.length() > 20) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名长度需≤20");
        }
        return t;
    }
}
