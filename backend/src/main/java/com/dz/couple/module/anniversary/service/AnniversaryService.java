package com.dz.couple.module.anniversary.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.anniversary.dto.AnniversaryCreateRequest;
import com.dz.couple.module.anniversary.dto.AnniversaryUpdateRequest;
import com.dz.couple.module.anniversary.dto.AnniversaryVO;
import com.dz.couple.module.anniversary.entity.Anniversary;
import com.dz.couple.module.anniversary.mapper.AnniversaryMapper;
import com.dz.couple.module.anniversary.util.AnniversaryDateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class AnniversaryService {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_REMINDER_DAYS = 30;

    private final AnniversaryMapper anniversaryMapper;

    @Autowired
    public AnniversaryService(AnniversaryMapper anniversaryMapper) {
        this.anniversaryMapper = anniversaryMapper;
    }

    public List<AnniversaryVO> list(Long coupleId) {
        List<Anniversary> all = anniversaryMapper.listAll(coupleId);
        List<AnniversaryVO> out = toVOs(all);
        out.sort(Comparator
                .comparing((AnniversaryVO v) -> v.getPinned() != null && v.getPinned() ? 0 : 1)
                .thenComparing(v -> v.getDaysLeft() == null ? Integer.MAX_VALUE : v.getDaysLeft())
                .thenComparing(v -> v.getTitle() == null ? "" : v.getTitle()));
        return out;
    }

    public List<AnniversaryVO> upcoming(Long coupleId, int days) {
        int d = Math.max(0, Math.min(days, 30));
        List<Anniversary> all = anniversaryMapper.listAll(coupleId);
        List<AnniversaryVO> out = new ArrayList<>();
        for (Anniversary a : all) {
            AnniversaryVO v = toVO(a);
            if (v.getDaysLeft() != null && v.getDaysLeft() <= d) {
                out.add(v);
            }
        }
        out.sort(Comparator
                .comparing((AnniversaryVO v) -> v.getPinned() != null && v.getPinned() ? 0 : 1)
                .thenComparing(v -> v.getDaysLeft() == null ? Integer.MAX_VALUE : v.getDaysLeft()));
        return out;
    }

    public AnniversaryVO create(Long userId, Long coupleId, AnniversaryCreateRequest req) {
        Anniversary a = new Anniversary();
        a.setCoupleId(coupleId);
        a.setTitle(safeTrim(req.getTitle()));
        String ct = normalizeCalendarType(req.getCalendarType());
        a.setCalendarType(ct);
        if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(ct)) {
            int m = clampLunarMonth(req.getLunarMonth());
            int d = clampLunarDay(req.getLunarDay());
            a.setLunarMonth(m);
            a.setLunarDay(d);
            a.setLunarLeap(req.getLunarLeap() != null && req.getLunarLeap() ? 1 : 0);
            a.setAnniversaryDate(null);
        } else {
            a.setAnniversaryDate(parseRequiredDate(req.getDate()));
            a.setLunarMonth(null);
            a.setLunarDay(null);
            a.setLunarLeap(0);
        }
        a.setType(safeTrim(req.getType()));
        a.setIcon(pickDefaultIfBlank(safeTrim(req.getIcon()), defaultIcon(a.getType())));
        a.setThemeColor(pickDefaultIfBlank(safeTrim(req.getThemeColor()), defaultThemeColor(a.getType())));
        a.setCoverUrl(safeTrim(req.getCoverUrl()));
        a.setCoverThumbUrl(safeTrim(req.getCoverThumbUrl()));
        a.setNote(safeTrim(req.getNote()));
        a.setReminderEnabled(req.getReminderEnabled() != null && !req.getReminderEnabled() ? 0 : 1);
        a.setReminderDaysBefore(clampReminderDays(req.getReminderDaysBefore()));
        a.setReminderOnDay(req.getReminderOnDay() != null && !req.getReminderOnDay() ? 0 : 1);
        a.setPinned(0);
        a.setCreatedBy(userId);
        Date now = new Date();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        int n = anniversaryMapper.insert(a);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return toVO(a);
    }

    public AnniversaryVO update(Long coupleId, Long id, AnniversaryUpdateRequest req) {
        Anniversary exist = anniversaryMapper.findById(coupleId, id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        exist.setTitle(safeTrim(req.getTitle()));
        String ct = normalizeCalendarType(req.getCalendarType());
        exist.setCalendarType(ct);
        if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(ct)) {
            int m = clampLunarMonth(req.getLunarMonth());
            int d = clampLunarDay(req.getLunarDay());
            exist.setLunarMonth(m);
            exist.setLunarDay(d);
            exist.setLunarLeap(req.getLunarLeap() != null && req.getLunarLeap() ? 1 : 0);
            exist.setAnniversaryDate(null);
        } else {
            exist.setAnniversaryDate(parseRequiredDate(req.getDate()));
            exist.setLunarMonth(null);
            exist.setLunarDay(null);
            exist.setLunarLeap(0);
        }
        exist.setType(safeTrim(req.getType()));
        exist.setIcon(pickDefaultIfBlank(safeTrim(req.getIcon()), defaultIcon(exist.getType())));
        exist.setThemeColor(pickDefaultIfBlank(safeTrim(req.getThemeColor()), defaultThemeColor(exist.getType())));
        exist.setCoverUrl(safeTrim(req.getCoverUrl()));
        exist.setCoverThumbUrl(safeTrim(req.getCoverThumbUrl()));
        exist.setNote(safeTrim(req.getNote()));
        exist.setReminderEnabled(req.getReminderEnabled() != null && !req.getReminderEnabled() ? 0 : 1);
        exist.setReminderDaysBefore(clampReminderDays(req.getReminderDaysBefore()));
        exist.setReminderOnDay(req.getReminderOnDay() != null && !req.getReminderOnDay() ? 0 : 1);
        exist.setUpdatedAt(new Date());
        int n = anniversaryMapper.update(exist);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return toVO(exist);
    }

    public void delete(Long coupleId, Long id) {
        Anniversary exist = anniversaryMapper.findById(coupleId, id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        anniversaryMapper.delete(coupleId, id);
    }

    public AnniversaryVO togglePinned(Long coupleId, Long id) {
        Anniversary exist = anniversaryMapper.findById(coupleId, id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Date now = new Date();
        if (exist.getPinned() != null && exist.getPinned() == 1) {
            anniversaryMapper.unsetPinned(coupleId, id, now);
            exist.setPinned(0);
            exist.setUpdatedAt(now);
            return toVO(exist);
        }
        anniversaryMapper.clearPinned(coupleId, now);
        anniversaryMapper.setPinned(coupleId, id, now);
        exist.setPinned(1);
        exist.setUpdatedAt(now);
        return toVO(exist);
    }

    private List<AnniversaryVO> toVOs(List<Anniversary> all) {
        List<AnniversaryVO> out = new ArrayList<>();
        if (all == null) {
            return out;
        }
        for (Anniversary a : all) {
            out.add(toVO(a));
        }
        return out;
    }

    private AnniversaryVO toVO(Anniversary a) {
        AnniversaryVO v = new AnniversaryVO();
        v.setId(a.getId());
        v.setTitle(a.getTitle());
        v.setDate(a.getAnniversaryDate());
        String ct = normalizeCalendarType(a.getCalendarType());
        v.setCalendarType(ct);
        v.setLunarMonth(a.getLunarMonth());
        v.setLunarDay(a.getLunarDay());
        v.setLunarLeap(a.getLunarLeap() != null && a.getLunarLeap() == 1);
        v.setType(a.getType());
        v.setIcon(pickDefaultIfBlank(a.getIcon(), defaultIcon(a.getType())));
        v.setThemeColor(pickDefaultIfBlank(a.getThemeColor(), defaultThemeColor(a.getType())));
        v.setCoverUrl(a.getCoverUrl());
        v.setCoverThumbUrl(a.getCoverThumbUrl());
        v.setNote(a.getNote());
        v.setReminderEnabled(a.getReminderEnabled() == null || a.getReminderEnabled() == 1);
        v.setReminderDaysBefore(clampReminderDays(a.getReminderDaysBefore()));
        v.setReminderOnDay(a.getReminderOnDay() == null || a.getReminderOnDay() == 1);
        v.setPinned(a.getPinned() != null && a.getPinned() == 1);
        LocalDate today = LocalDate.now();
        AnniversaryDateUtil.Occurrence occ;
        if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(ct)) {
            occ = AnniversaryDateUtil.lunarOccurrence(today, a.getLunarMonth(), a.getLunarDay(), a.getLunarLeap());
        } else {
            occ = AnniversaryDateUtil.solarOccurrence(today, a.getAnniversaryDate());
        }
        if (occ != null) {
            v.setNextDate(AnniversaryDateUtil.toDate(occ.getNextDate()));
            v.setDaysLeft(occ.getDaysLeft());
        }
        return v;
    }

    private Date parseRequiredDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择日期");
        }
        try {
            LocalDate d = LocalDate.parse(s, DF);
            return AnniversaryDateUtil.toDate(d);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期格式需为 yyyy-MM-dd");
        }
    }

    private String normalizeCalendarType(String s) {
        String t = safeTrim(s);
        if (t == null) {
            return AnniversaryDateUtil.CALENDAR_SOLAR;
        }
        String u = t.toUpperCase();
        if (AnniversaryDateUtil.CALENDAR_LUNAR.equals(u)) {
            return AnniversaryDateUtil.CALENDAR_LUNAR;
        }
        return AnniversaryDateUtil.CALENDAR_SOLAR;
    }

    private int clampLunarMonth(Integer v) {
        if (v == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择农历月份");
        }
        int n = v;
        if (n < 1 || n > 12) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "农历月份需为 1-12");
        }
        return n;
    }

    private int clampLunarDay(Integer v) {
        if (v == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择农历日期");
        }
        int n = v;
        if (n < 1 || n > 30) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "农历日期需为 1-30");
        }
        return n;
    }

    private String safeTrim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Integer clampReminderDays(Integer v) {
        if (v == null) {
            return 3;
        }
        int n = v;
        if (n < 0) {
            n = 0;
        }
        if (n > MAX_REMINDER_DAYS) {
            n = MAX_REMINDER_DAYS;
        }
        return n;
    }

    private String pickDefaultIfBlank(String v, String def) {
        String s = safeTrim(v);
        return s == null ? def : s;
    }

    private String defaultIcon(String type) {
        String t = safeTrim(type);
        if (t == null) {
            return "✨";
        }
        if (t.contains("生日")) {
            return "🎂";
        }
        if (t.contains("春节")) {
            return "🧨";
        }
        if (t.contains("元宵")) {
            return "🏮";
        }
        if (t.contains("端午")) {
            return "🐉";
        }
        if (t.contains("七夕")) {
            return "💞";
        }
        if (t.contains("中秋")) {
            return "🌕";
        }
        if (t.contains("重阳")) {
            return "🍂";
        }
        if (t.contains("周年") || t.contains("纪念")) {
            return "💍";
        }
        if (t.contains("旅行")) {
            return "✈️";
        }
        if (t.contains("第一次")) {
            return "🌟";
        }
        return "✨";
    }

    private String defaultThemeColor(String type) {
        String t = safeTrim(type);
        if (t == null) {
            return "#6366F1";
        }
        if (t.contains("生日")) {
            return "#F59E0B";
        }
        if (t.contains("春节")) {
            return "#EF4444";
        }
        if (t.contains("元宵")) {
            return "#F59E0B";
        }
        if (t.contains("端午")) {
            return "#10B981";
        }
        if (t.contains("七夕")) {
            return "#EC4899";
        }
        if (t.contains("中秋")) {
            return "#6366F1";
        }
        if (t.contains("重阳")) {
            return "#F97316";
        }
        if (t.contains("周年") || t.contains("纪念")) {
            return "#EC4899";
        }
        if (t.contains("旅行")) {
            return "#10B981";
        }
        if (t.contains("第一次")) {
            return "#8B5CF6";
        }
        return "#6366F1";
    }
}
