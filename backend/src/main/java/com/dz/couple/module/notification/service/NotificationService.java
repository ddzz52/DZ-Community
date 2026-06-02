package com.dz.couple.module.notification.service;

import com.dz.couple.module.anniversary.entity.Anniversary;
import com.dz.couple.module.anniversary.mapper.AnniversaryMapper;
import com.dz.couple.module.anniversary.util.AnniversaryDateUtil;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.dto.NotificationVO;
import com.dz.couple.module.notification.entity.Notification;
import com.dz.couple.module.notification.mapper.NotificationMapper;
import com.dz.couple.module.profile.entity.UserSettings;
import com.dz.couple.module.profile.mapper.UserSettingsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    private static final int ARCHIVE_DAYS = 30;
    private final NotificationMapper notificationMapper;
    private final AnniversaryMapper anniversaryMapper;
    private final UserSettingsMapper userSettingsMapper;

    @Autowired
    public NotificationService(NotificationMapper notificationMapper, AnniversaryMapper anniversaryMapper, UserSettingsMapper userSettingsMapper) {
        this.notificationMapper = notificationMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.userSettingsMapper = userSettingsMapper;
    }

    public void ensureAnniversaryReminders(Long userId, Long coupleId) {
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        if (settings != null && settings.getReminderEnabled() != null && settings.getReminderEnabled() == 0) {
            return;
        }
        archiveExpired(userId);
        if (settings != null) {
            String ws = settings.getReminderWindowStart();
            String we = settings.getReminderWindowEnd();
            if (ws != null || we != null) {
                if (!isInWindow(ws, we)) {
                    return;
                }
            }
        }
        boolean silent = settings != null && settings.getDndEnabled() != null && settings.getDndEnabled() == 1 && isInDnd(settings.getDndStart(), settings.getDndEnd());
        List<Anniversary> all = anniversaryMapper.listAll(coupleId);
        if (all == null || all.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (Anniversary a : all) {
            if (a == null) {
                continue;
            }
            if (a.getReminderEnabled() != null && a.getReminderEnabled() == 0) {
                continue;
            }
            String ct = a.getCalendarType() == null ? AnniversaryDateUtil.CALENDAR_SOLAR : a.getCalendarType().trim().toUpperCase();
            AnniversaryDateUtil.Occurrence occ;
            if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(ct)) {
                occ = AnniversaryDateUtil.lunarOccurrence(today, a.getLunarMonth(), a.getLunarDay(), a.getLunarLeap());
            } else {
                occ = AnniversaryDateUtil.solarOccurrence(today, a.getAnniversaryDate());
            }
            if (occ == null || occ.getNextDate() == null) {
                continue;
            }
            LocalDate next = occ.getNextDate();
            int daysLeft = occ.getDaysLeft();
            int before = a.getReminderDaysBefore() == null ? 3 : a.getReminderDaysBefore();
            if (before < 0) {
                before = 0;
            }
            if (before > 30) {
                before = 30;
            }
            boolean onDay = a.getReminderOnDay() == null || a.getReminderOnDay() == 1;
            boolean isTriggerDay = (onDay && next.equals(today)) || (before > 0 && next.minusDays(before).equals(today));
            boolean shouldNotify = isTriggerDay;
            if (!shouldNotify) {
                continue;
            }
            Notification n = new Notification();
            n.setUserId(userId);
            n.setCoupleId(coupleId);
            n.setType(NotificationTypes.ANNIVERSARY_REMINDER);
            n.setTitle("纪念日提醒：" + a.getTitle());
            String md = next.getMonthValue() + "-" + String.format("%02d", next.getDayOfMonth());
            if (daysLeft == 0) {
                n.setContent("今天 · " + md);
            } else {
                n.setContent("还有 " + daysLeft + " 天 · " + md);
            }
            n.setRefId(a.getId());
            n.setRefDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            n.setReadFlag(0);
            n.setSilentFlag(silent ? 1 : 0);
            n.setArchivedFlag(0);
            notificationMapper.upsert(n);
        }
    }

    private boolean isInDnd(String start, String end) {
        if (start == null || end == null) {
            return false;
        }
        if (!isTimeFormat(start) || !isTimeFormat(end)) {
            return false;
        }
        LocalTime s = LocalTime.parse(start);
        LocalTime e = LocalTime.parse(end);
        LocalTime now = LocalTime.now();
        if (s.equals(e)) {
            return true;
        }
        if (s.isBefore(e)) {
            return !now.isBefore(s) && now.isBefore(e);
        }
        return !now.isBefore(s) || now.isBefore(e);
    }

    private boolean isInWindow(String start, String end) {
        if (start == null || end == null) {
            return true;
        }
        if (!isTimeFormat(start) || !isTimeFormat(end)) {
            return true;
        }
        LocalTime s = LocalTime.parse(start);
        LocalTime e = LocalTime.parse(end);
        LocalTime now = LocalTime.now();
        if (s.equals(e)) {
            return true;
        }
        if (s.isBefore(e)) {
            return !now.isBefore(s) && now.isBefore(e);
        }
        return !now.isBefore(s) || now.isBefore(e);
    }

    private boolean isTimeFormat(String s) {
        return s != null && s.matches("^\\d{2}:\\d{2}$");
    }

    public List<NotificationVO> listMy(Long userId, boolean unreadOnly, int limit) {
        archiveExpired(userId);
        int u = unreadOnly ? 1 : 0;
        int l = limit <= 0 ? 50 : Math.min(limit, 200);
        List<Notification> list = notificationMapper.listByUserId(userId, u, l);
        List<NotificationVO> out = new ArrayList<>();
        if (list != null) {
            for (Notification n : list) {
                out.add(toVO(n));
            }
        }
        return out;
    }

    public int markRead(Long userId, Long id) {
        return notificationMapper.markRead(userId, id);
    }

    public int markReadAll(Long userId) {
        return notificationMapper.markReadAll(userId);
    }

    public int clearRead(Long userId) {
        archiveExpired(userId);
        return notificationMapper.archiveRead(userId);
    }

    public int countUnreadAll(Long userId) {
        archiveExpired(userId);
        return notificationMapper.countUnreadAll(userId);
    }

    public int countUnreadReminders(Long userId) {
        archiveExpired(userId);
        return notificationMapper.countUnreadByType(userId, NotificationTypes.ANNIVERSARY_REMINDER)
                + notificationMapper.countUnreadByType(userId, NotificationTypes.PERIOD_REMINDER)
                + notificationMapper.countUnreadByType(userId, NotificationTypes.OVULATION_REMINDER)
                + notificationMapper.countUnreadByType(userId, NotificationTypes.WISH_ROULETTE);
    }

    public void create(Long userId, Long coupleId, String type, String title, String content, Long refId, Date refDate, boolean upsert) {
        if (userId == null || coupleId == null) {
            return;
        }
        archiveExpired(userId);
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        boolean silent = settings != null && settings.getDndEnabled() != null && settings.getDndEnabled() == 1 && isInDnd(settings.getDndStart(), settings.getDndEnd());
        Notification n = new Notification();
        n.setUserId(userId);
        n.setCoupleId(coupleId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRefId(refId);
        n.setRefDate(refDate);
        n.setReadFlag(0);
        n.setSilentFlag(silent ? 1 : 0);
        n.setArchivedFlag(0);
        if (upsert) {
            notificationMapper.upsert(n);
        } else {
            notificationMapper.insert(n);
        }
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setRead(n.getReadFlag() != null && n.getReadFlag() == 1);
        vo.setSilent(n.getSilentFlag() != null && n.getSilentFlag() == 1);
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }

    private void archiveExpired(Long userId) {
        if (userId == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate cutoffDay = today.minusDays(ARCHIVE_DAYS);
        Date cutoff = Date.from(cutoffDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
        notificationMapper.archiveOlderThan(userId, cutoff);
    }
}
