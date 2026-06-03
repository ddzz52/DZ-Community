package com.dz.couple.module.profile.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CacheService;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.anniversary.mapper.AnniversaryMapper;
import com.dz.couple.module.couple.entity.Couple;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.diary.mapper.DiaryMapper;
import com.dz.couple.module.message.ws.ChatHub;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.photo.mapper.PhotoMapper;
import com.dz.couple.module.profile.dto.ProfileResponse;
import com.dz.couple.module.profile.dto.ChangePasswordRequest;
import com.dz.couple.module.profile.dto.UpdateMySignatureRequest;
import com.dz.couple.module.profile.dto.UpdateProfileRequest;
import com.dz.couple.module.profile.dto.UpdateSettingsRequest;
import com.dz.couple.module.profile.dto.UpdateTempSignatureRequest;
import com.dz.couple.module.profile.dto.UserSettingsVO;
import com.dz.couple.module.profile.entity.UserSettings;
import com.dz.couple.module.profile.mapper.UserSettingsMapper;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class ProfileService {
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final AnniversaryMapper anniversaryMapper;
    private final DiaryMapper diaryMapper;
    private final PhotoMapper photoMapper;
    private final PasswordUtil passwordUtil;
    private final UserSettingsMapper userSettingsMapper;
    private final NotificationService notificationService;
    private final ChatHub chatHub;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "cache:profile:";
    private static final long CACHE_TTL_SEC = 15;

    @Autowired
    public ProfileService(UserMapper userMapper, CoupleMapper coupleMapper, AnniversaryMapper anniversaryMapper, DiaryMapper diaryMapper, PhotoMapper photoMapper, PasswordUtil passwordUtil, UserSettingsMapper userSettingsMapper, NotificationService notificationService, ChatHub chatHub, CacheService cacheService) {
        this.userMapper = userMapper;
        this.coupleMapper = coupleMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.diaryMapper = diaryMapper;
        this.photoMapper = photoMapper;
        this.passwordUtil = passwordUtil;
        this.userSettingsMapper = userSettingsMapper;
        this.notificationService = notificationService;
        this.chatHub = chatHub;
        this.cacheService = cacheService;
    }

    public ProfileResponse getProfile(Long userId, Long coupleId) {
        // Redis 缓存 15 秒 — key 必须包含 userId，防止同 couple 串号
        String cacheKey = CACHE_PREFIX + coupleId + ":" + userId;
        ProfileResponse cached = cacheService.get(cacheKey, ProfileResponse.class);
        if (cached != null && cached.getMe() != null) return cached;
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        User partner = null;
        for (User u : users) {
            if (u != null && !u.getId().equals(userId)) {
                partner = u;
                break;
            }
        }

        Couple couple = coupleMapper.findById(coupleId);

        Date loveDate = pickLoveDate(users);
        int loveDays = loveDate == null ? 0 : calcLoveDays(loveDate);
        String loveText = loveDays <= 0 ? "" : buildLoveText(loveDays);

        ProfileResponse resp = new ProfileResponse();
        resp.setSignature(couple == null ? null : couple.getSignature());
        resp.setLoveDays(loveDays);
        resp.setLoveAnniversaryText(loveText);
        ProfileResponse.Stats stats = new ProfileResponse.Stats();
        stats.setAnniversaries(anniversaryMapper.countByCoupleId(coupleId));
        stats.setDiaries(diaryMapper.countByCoupleId(coupleId));
        stats.setPhotos(photoMapper.countByCoupleId(coupleId));
        resp.setStats(stats);
        resp.setMe(toVO(me));
        resp.setPartner(partner == null ? null : toVO(partner));
        // 在线状态：通过 WebSocket ChatHub 判断
        resp.setPartnerOnline(partner != null && chatHub.hasOnline(partner.getId()));
        // 写入缓存（partnerOnline 不缓存，每次实时查）
        cacheService.set(cacheKey, resp, CACHE_TTL_SEC);
        return resp;
    }

    /** Profile 数据变更后清除缓存 */
    public void evictCache(Long coupleId) {
        cacheService.delete(CACHE_PREFIX + coupleId);
    }

    @Transactional
    public UserVO updateMe(Long userId, Long coupleId, UpdateProfileRequest req) {
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userMapper.updateProfile(userId, req.getNickname(), req.getAvatarUrl(), req.getGender(), req.getLoveDate(), req.getZodiac());
        User updated = userMapper.findById(userId);
        return toVO(updated);
    }

    @Transactional
    public void updateSignature(Long coupleId, String signature) {
        coupleMapper.updateSignature(coupleId, signature);
    }

    @Transactional
    public UserVO updateMySignature(Long userId, Long coupleId, UpdateMySignatureRequest req) {
        ensureMember(userId, coupleId);
        String s = safeTrim(req == null ? null : req.getSignature());
        userMapper.updateSignature(userId, s);
        User updated = userMapper.findById(userId);
        return toVO(updated);
    }

    @Transactional
    public UserVO updateTempSignature(Long userId, Long coupleId, UpdateTempSignatureRequest req) {
        ensureMember(userId, coupleId);
        String temp = safeTrim(req == null ? null : req.getTempSignature());
        Integer days = req == null ? null : req.getExpireDays();
        if (days != null && days != 1 && days != 3 && days != 7) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "有效期仅支持1/3/7天或永久");
        }
        Date expireTime = null;
        if (temp != null && !temp.isEmpty()) {
            if (days != null) {
                LocalDateTime dt = LocalDateTime.now().plusDays(days);
                expireTime = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
            }
        } else {
            temp = null;
            expireTime = null;
        }
        userMapper.updateTempSignature(userId, temp, expireTime);
        User updated = userMapper.findById(userId);
        return toVO(updated);
    }

    @Transactional
    public void changePassword(Long userId, Long coupleId, ChangePasswordRequest req) {
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordUtil.matches(req.getOldPassword(), me.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        validatePassword(req.getNewPassword());
        userMapper.updatePasswordHash(userId, passwordUtil.hash(req.getNewPassword()));
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users != null) {
            for (User u : users) {
                if (u == null || u.getId() == null) {
                    continue;
                }
                notificationService.create(
                        u.getId(),
                        coupleId,
                        NotificationTypes.SECURITY_PASSWORD_CHANGED,
                        "密码已修改",
                        "你的账号密码已在“我的-安全”中修改。",
                        userId,
                        today,
                        true
                );
            }
        }
    }

    public UserSettingsVO getSettings(Long userId, Long coupleId) {
        ensureMember(userId, coupleId);
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        if (settings == null) {
            settings = defaultSettings(userId, coupleId);
            userSettingsMapper.insert(settings);
        }
        return toVO(settings);
    }

    @Transactional
    public UserSettingsVO updateSettings(Long userId, Long coupleId, UpdateSettingsRequest req) {
        ensureMember(userId, coupleId);
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        if (settings == null) {
            settings = defaultSettings(userId, coupleId);
            userSettingsMapper.insert(settings);
        }
        if (req.getMessageEnabled() != null) {
            settings.setMessageEnabled(req.getMessageEnabled() ? 1 : 0);
        }
        if (req.getReminderEnabled() != null) {
            settings.setReminderEnabled(req.getReminderEnabled() ? 1 : 0);
        }
        if (req.getReminderWindowStart() != null) {
            settings.setReminderWindowStart(req.getReminderWindowStart());
        }
        if (req.getReminderWindowEnd() != null) {
            settings.setReminderWindowEnd(req.getReminderWindowEnd());
        }
        if (req.getDndEnabled() != null) {
            settings.setDndEnabled(req.getDndEnabled() ? 1 : 0);
        }
        if (req.getDndStart() != null) {
            settings.setDndStart(req.getDndStart());
        }
        if (req.getDndEnd() != null) {
            settings.setDndEnd(req.getDndEnd());
        }
        ensureDndPairWhenEnabled(settings);
        if (settings.getDndEnabled() != null && settings.getDndEnabled() == 1) {
            validateTime(settings.getDndStart(), "免打扰开始时间格式错误");
            validateTime(settings.getDndEnd(), "免打扰结束时间格式错误");
        }
        boolean reminderOn = settings.getReminderEnabled() != null && settings.getReminderEnabled() == 1;
        if (reminderOn) {
            if (hasTimeValue(settings.getReminderWindowStart())) {
                validateTime(settings.getReminderWindowStart(), "提醒开始时间格式错误");
            }
            if (hasTimeValue(settings.getReminderWindowEnd())) {
                validateTime(settings.getReminderWindowEnd(), "提醒结束时间格式错误");
            }
        }
        userSettingsMapper.update(settings);
        return toVO(settings);
    }

    private void validatePassword(String pwd) {
        if (pwd == null || pwd.length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度需≥8位且包含字母、数字、特殊符号");
        }
        boolean hasLetter = pwd.matches(".*[A-Za-z].*");
        boolean hasDigit = pwd.matches(".*\\d.*");
        boolean hasSymbol = pwd.matches(".*[^A-Za-z0-9].*");
        if (!hasLetter || !hasDigit || !hasSymbol) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码需包含字母、数字、特殊符号");
        }
    }

    private void validateTime(String s, String msg) {
        if (s == null || !s.matches("^\\d{2}:\\d{2}$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, msg);
        }
    }

    private static boolean hasTimeValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void ensureDndPairWhenEnabled(UserSettings settings) {
        if (settings.getDndEnabled() == null || settings.getDndEnabled() != 1) {
            return;
        }
        if (!hasTimeValue(settings.getDndStart())) {
            settings.setDndStart("22:00");
        }
        if (!hasTimeValue(settings.getDndEnd())) {
            settings.setDndEnd("08:00");
        }
    }

    private void ensureMember(Long userId, Long coupleId) {
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private UserSettings defaultSettings(Long userId, Long coupleId) {
        UserSettings s = new UserSettings();
        s.setUserId(userId);
        s.setCoupleId(coupleId);
        s.setMessageEnabled(1);
        s.setReminderEnabled(1);
        s.setReminderWindowStart(null);
        s.setReminderWindowEnd(null);
        s.setDndEnabled(0);
        s.setDndStart(null);
        s.setDndEnd(null);
        return s;
    }

    private UserSettingsVO toVO(UserSettings s) {
        UserSettingsVO vo = new UserSettingsVO();
        vo.setMessageEnabled(s.getMessageEnabled() != null && s.getMessageEnabled() == 1);
        vo.setReminderEnabled(s.getReminderEnabled() != null && s.getReminderEnabled() == 1);
        vo.setReminderWindowStart(s.getReminderWindowStart());
        vo.setReminderWindowEnd(s.getReminderWindowEnd());
        vo.setDndEnabled(s.getDndEnabled() != null && s.getDndEnabled() == 1);
        vo.setDndStart(s.getDndStart());
        vo.setDndEnd(s.getDndEnd());
        return vo;
    }

    private Date pickLoveDate(List<User> users) {
        Date best = null;
        if (users == null) {
            return null;
        }
        for (User u : users) {
            if (u == null || u.getLoveDate() == null) {
                continue;
            }
            if (best == null || u.getLoveDate().before(best)) {
                best = u.getLoveDate();
            }
        }
        return best;
    }

    private int calcLoveDays(Date loveDate) {
        LocalDate start = loveDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(start, today) + 1;
        if (days < 0) {
            return 0;
        }
        return (int) days;
    }

    private String buildLoveText(int loveDays) {
        int years = loveDays / 365;
        if (years <= 0) {
            return loveDays + " 天";
        }
        return years + " 周年 · " + loveDays + " 天";
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setCoupleId(user.getCoupleId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setRole(user.getRole());
        vo.setLoveDate(user.getLoveDate());
        vo.setZodiac(user.getZodiac());
        vo.setSignature(user.getSignature());
        vo.setTempSignature(user.getTempSignature());
        vo.setSignatureExpireTime(user.getSignatureExpireTime());
        vo.setEffectiveSignature(calcEffectiveSignature(user));
        return vo;
    }

    private String calcEffectiveSignature(User user) {
        if (user == null) {
            return null;
        }
        String temp = safeTrim(user.getTempSignature());
        Date exp = user.getSignatureExpireTime();
        if (temp != null && !temp.isEmpty()) {
            if (exp == null) {
                return temp;
            }
            if (exp.after(new Date())) {
                return temp;
            }
        }
        return safeTrim(user.getSignature());
    }

    private String safeTrim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
