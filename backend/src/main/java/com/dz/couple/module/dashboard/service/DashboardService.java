package com.dz.couple.module.dashboard.service;

import com.dz.couple.common.CacheService;
import com.dz.couple.module.anniversary.entity.Anniversary;
import com.dz.couple.module.anniversary.mapper.AnniversaryMapper;
import com.dz.couple.module.anniversary.util.AnniversaryDateUtil;
import com.dz.couple.module.dashboard.dto.DashboardResponse;
import com.dz.couple.module.diary.entity.Diary;
import com.dz.couple.module.diary.mapper.DiaryMapper;
import com.dz.couple.module.message.service.MessageService;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.photo.entity.Photo;
import com.dz.couple.module.photo.mapper.PhotoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class DashboardService {
    private final AnniversaryMapper anniversaryMapper;
    private final DiaryMapper diaryMapper;
    private final PhotoMapper photoMapper;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "cache:dashboard:";
    private static final long CACHE_TTL_SEC = 30;

    @Autowired
    public DashboardService(AnniversaryMapper anniversaryMapper, DiaryMapper diaryMapper, PhotoMapper photoMapper,
                            NotificationService notificationService, MessageService messageService, CacheService cacheService) {
        this.anniversaryMapper = anniversaryMapper;
        this.diaryMapper = diaryMapper;
        this.photoMapper = photoMapper;
        this.notificationService = notificationService;
        this.messageService = messageService;
        this.cacheService = cacheService;
    }

    public DashboardResponse getDashboard(Long userId, Long coupleId) {
        // Redis 缓存 30 秒，高频刷首页不再重复查 DB
        String cacheKey = CACHE_PREFIX + coupleId;
        DashboardResponse cached = cacheService.get(cacheKey, DashboardResponse.class);
        if (cached != null) return cached;

        DashboardResponse resp = new DashboardResponse();
        resp.setToday(buildToday());
        resp.setAnniversary(buildAnniversary(coupleId));
        resp.setCoverPhoto(buildCoverPhoto(coupleId));
        resp.setRecent(buildRecent(coupleId));
        resp.setBadges(buildBadges(userId, coupleId));

        cacheService.set(cacheKey, resp, CACHE_TTL_SEC);
        return resp;
    }

    /** 数据变更后清除 Dashboard 缓存 */
    public void evictCache(Long coupleId) {
        cacheService.delete(CACHE_PREFIX + coupleId);
    }

    private DashboardResponse.Today buildToday() {
        DashboardResponse.Today today = new DashboardResponse.Today();
        today.setDate(new Date());
        today.setQuote(pickQuote());
        return today;
    }

    private String pickQuote() {
        List<String> quotes = Arrays.asList(
                "把今天过成我们喜欢的样子",
                "记录会让幸福更具体",
                "愿你们的日常都闪着光",
                "今天也一起加油",
                "不畏浮云遮望眼，只缘身在最高层",
                "静以修身，俭以养德",
                "执子之手，与子偕老",
                "山有木兮木有枝，心悦君兮君不知",
                "If I know what love is, it is because of you",
                "Love looks not with the eyes, but with the mind",
                "小小的偏爱，刚刚好"
        );
        int idx = (int) (System.currentTimeMillis() % quotes.size());
        return quotes.get(idx);
    }

    private DashboardResponse.AnniversaryCard buildAnniversary(Long coupleId) {
        Anniversary pinned = anniversaryMapper.findPinned(coupleId);
        Anniversary chosen = pinned;
        if (chosen == null) {
            List<Anniversary> all = anniversaryMapper.listAll(coupleId);
            chosen = pickNext(all);
        }
        if (chosen == null) return null;
        LocalDate today = LocalDate.now();
        LocalDate next = nextDateOf(chosen, today);
        if (next == null) return null;
        DashboardResponse.AnniversaryCard card = new DashboardResponse.AnniversaryCard();
        card.setId(chosen.getId()); card.setTitle(chosen.getTitle());
        card.setDate(Date.from(next.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        card.setDaysLeft((int) ChronoUnit.DAYS.between(today, next));
        return card;
    }

    private Anniversary pickNext(List<Anniversary> all) {
        if (all == null || all.isEmpty()) return null;
        LocalDate today = LocalDate.now();
        Anniversary best = null;
        long bestDays = Long.MAX_VALUE;
        for (Anniversary a : all) {
            if (a == null) continue;
            LocalDate next = nextDateOf(a, today);
            if (next == null) continue;
            long days = ChronoUnit.DAYS.between(today, next);
            if (days < bestDays) { bestDays = days; best = a; }
        }
        return best;
    }

    private LocalDate nextDateOf(Anniversary a, LocalDate today) {
        if (a == null || today == null) return null;
        String ct = a.getCalendarType() == null ? AnniversaryDateUtil.CALENDAR_SOLAR : a.getCalendarType().trim().toUpperCase();
        AnniversaryDateUtil.Occurrence occ;
        if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(ct))
            occ = AnniversaryDateUtil.lunarOccurrence(today, a.getLunarMonth(), a.getLunarDay(), a.getLunarLeap());
        else
            occ = AnniversaryDateUtil.solarOccurrence(today, a.getAnniversaryDate());
        return occ == null ? null : occ.getNextDate();
    }

    private DashboardResponse.Recent buildRecent(Long coupleId) {
        DashboardResponse.Recent recent = new DashboardResponse.Recent();
        List<Diary> diaries = diaryMapper.listRecent(coupleId, 3);
        List<DashboardResponse.DiaryPreview> diaryViews = new ArrayList<>();
        if (diaries != null) for (Diary d : diaries) {
            DashboardResponse.DiaryPreview v = new DashboardResponse.DiaryPreview();
            v.setId(d.getId()); v.setMood(d.getMood());
            v.setCreatedAt(d.getCreatedAt()); v.setContentPreview(preview(d.getContent()));
            diaryViews.add(v);
        }
        recent.setDiaries(diaryViews);
        List<Photo> photos = photoMapper.listRecent(coupleId, 3);
        List<DashboardResponse.PhotoPreview> photoViews = new ArrayList<>();
        if (photos != null) for (Photo p : photos) {
            DashboardResponse.PhotoPreview v = new DashboardResponse.PhotoPreview();
            v.setId(p.getId()); v.setUrl(p.getUrl()); v.setThumbUrl(p.getThumbUrl());
            v.setCreatedAt(p.getCreatedAt()); photoViews.add(v);
        }
        recent.setPhotos(photoViews);
        return recent;
    }

    private DashboardResponse.PhotoPreview buildCoverPhoto(Long coupleId) {
        Photo p = photoMapper.findCover(coupleId);
        if (p == null) return null;
        DashboardResponse.PhotoPreview v = new DashboardResponse.PhotoPreview();
        v.setId(p.getId()); v.setUrl(p.getUrl()); v.setThumbUrl(p.getThumbUrl());
        v.setCreatedAt(p.getCreatedAt());
        return v;
    }

    private String preview(String content) {
        if (content == null) return "";
        String c = content.trim().replaceAll("\\s+", " ");
        return c.length() <= 80 ? c : c.substring(0, 80) + "…";
    }

    /** buildBadges — 纯读操作，不再触发 DB 写（纪念日提醒/经期提醒由定时任务负责） */
    private DashboardResponse.Badges buildBadges(Long userId, Long coupleId) {
        int reminders = notificationService.countUnreadReminders(userId);
        int all = notificationService.countUnreadAll(userId);
        int unreadNotifications = Math.max(0, all - reminders);
        int unreadMessages = messageService.countUnread(userId, coupleId);
        DashboardResponse.Badges badges = new DashboardResponse.Badges();
        badges.setUnreadMessages(unreadMessages);
        badges.setUnreadNotifications(unreadNotifications);
        badges.setReminders(reminders);
        return badges;
    }
}
