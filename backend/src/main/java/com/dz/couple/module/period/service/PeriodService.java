package com.dz.couple.module.period.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.period.dto.ConfirmPeriodRequest;
import com.dz.couple.module.period.dto.PeriodPredictionVO;
import com.dz.couple.module.period.dto.PeriodSettingsVO;
import com.dz.couple.module.period.dto.PeriodStatusVO;
import com.dz.couple.module.period.dto.UpdatePeriodReminderRequest;
import com.dz.couple.module.period.dto.UpdatePeriodSettingsRequest;
import com.dz.couple.module.period.entity.PeriodSettings;
import com.dz.couple.module.period.mapper.PeriodSettingsMapper;
import com.dz.couple.module.profile.entity.UserSettings;
import com.dz.couple.module.profile.mapper.UserSettingsMapper;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.CryptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class PeriodService {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Logger log = LoggerFactory.getLogger(PeriodService.class);
    private final PeriodSettingsMapper periodSettingsMapper;
    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final NotificationService notificationService;
    private final CryptoUtil cryptoUtil;
    private final ObjectMapper objectMapper;

    @Autowired
    public PeriodService(PeriodSettingsMapper periodSettingsMapper,
                         UserMapper userMapper,
                         UserSettingsMapper userSettingsMapper,
                         NotificationService notificationService,
                         CryptoUtil cryptoUtil,
                         ObjectMapper objectMapper) {
        this.periodSettingsMapper = periodSettingsMapper;
        this.userMapper = userMapper;
        this.userSettingsMapper = userSettingsMapper;
        this.notificationService = notificationService;
        this.cryptoUtil = cryptoUtil;
        this.objectMapper = objectMapper;
    }

    public PeriodStatusVO getStatus(Long coupleId) {
        PeriodSettings s = periodSettingsMapper.findByCoupleId(coupleId);
        PeriodStatusVO out = new PeriodStatusVO();
        if (s == null || s.getEncData() == null || s.getEncData().trim().isEmpty()) {
            out.setSettings(null);
            out.setPrediction(null);
            return out;
        }
        EncData d = safeReadEncData(s);
        if (d == null) {
            out.setSettings(null);
            out.setPrediction(null);
            return out;
        }
        PeriodSettingsVO svo = new PeriodSettingsVO();
        svo.setCycleDays(d.cycleDays);
        svo.setPeriodDays(d.periodDays);
        svo.setLastStartDate(d.lastStartDate);
        svo.setReminderEnabled(d.reminderEnabled == null ? Boolean.TRUE : d.reminderEnabled);
        svo.setOwnerUserId(s.getOwnerUserId());
        User owner = s.getOwnerUserId() == null ? null : userMapper.findById(s.getOwnerUserId());
        svo.setOwnerNickname(owner == null ? null : owner.getNickname());
        svo.setUpdatedAt(s.getUpdatedAt());
        out.setSettings(svo);
        out.setPrediction(safeBuildPrediction(d, coupleId));
        return out;
    }

    public PeriodStatusVO updateSettings(Long userId, Long coupleId, UpdatePeriodSettingsRequest req) {
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (req == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        int cycleDays = req.getCycleDays() == null ? 28 : req.getCycleDays();
        int periodDays = req.getPeriodDays() == null ? 5 : req.getPeriodDays();
        if (cycleDays < 25 || cycleDays > 45) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (periodDays < 3 || periodDays > 7) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String lastStartDate = req.getLastStartDate();
        if (lastStartDate == null || lastStartDate.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上次经期开始日期不能为空");
        }
        LocalDate lastParsed;
        try {
            lastParsed = LocalDate.parse(lastStartDate.trim(), DTF);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上次经期开始日期格式需为 yyyy-MM-dd");
        }
        LocalDate today = LocalDate.now();
        if (lastParsed.isAfter(today)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上次经期开始日期不能晚于今天");
        }

        PeriodSettings s = periodSettingsMapper.findByCoupleId(coupleId);
        EncData d = s == null ? null : safeReadEncData(s);
        EncData next = new EncData();
        next.cycleDays = cycleDays;
        next.periodDays = periodDays;
        next.lastStartDate = lastParsed.format(DTF);
        next.reminderEnabled = d == null ? Boolean.TRUE : (d.reminderEnabled == null ? Boolean.TRUE : d.reminderEnabled);
        String enc = writeEncData(next);

        if (s == null) {
            s = new PeriodSettings();
            s.setCoupleId(coupleId);
            s.setOwnerUserId(userId);
            s.setEncData(enc);
            s.setCreatedBy(userId);
            s.setUpdatedBy(userId);
            periodSettingsMapper.insert(s);
        } else {
            s.setOwnerUserId(userId);
            s.setEncData(enc);
            s.setUpdatedBy(userId);
            periodSettingsMapper.updateByCoupleId(s);
        }
        return getStatus(coupleId);
    }

    public PeriodStatusVO updateReminder(Long userId, Long coupleId, UpdatePeriodReminderRequest req) {
        if (req == null || req.getEnabled() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        PeriodSettings s = periodSettingsMapper.findByCoupleId(coupleId);
        if (s == null) {
            return getStatus(coupleId);
        }
        EncData d = safeReadEncData(s);
        if (d == null) {
            return getStatus(coupleId);
        }
        d.reminderEnabled = req.getEnabled();
        s.setEncData(writeEncData(d));
        s.setUpdatedBy(userId);
        periodSettingsMapper.updateByCoupleId(s);
        return getStatus(coupleId);
    }

    public PeriodStatusVO confirmPeriod(Long userId, Long coupleId, ConfirmPeriodRequest req) {
        if (req == null || req.getAction() == null || req.getAction().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "action 不能为空");
        }
        String action = req.getAction().trim();
        if (!"started".equals(action) && !"delayed".equals(action)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "action 只能是 started 或 delayed");
        }
        PeriodSettings s = periodSettingsMapper.findByCoupleId(coupleId);
        if (s == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成周期设置");
        }
        EncData d = safeReadEncData(s);
        if (d == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成周期设置");
        }
        LocalDate today = LocalDate.now();
        if ("started".equals(action)) {
            String dateStr = req.getDate();
            LocalDate confirmDate;
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                try {
                    confirmDate = LocalDate.parse(dateStr.trim(), DTF);
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "日期格式需为 yyyy-MM-dd");
                }
                if (confirmDate.isAfter(today)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "确认日期不能晚于今天");
                }
            } else {
                confirmDate = today;
            }
            d.lastStartDate = confirmDate.format(DTF);
            d.confirmedDelayDate = null;
        } else {
            d.confirmedDelayDate = today.format(DTF);
        }
        s.setEncData(writeEncData(d));
        s.setUpdatedBy(userId);
        periodSettingsMapper.updateByCoupleId(s);
        return getStatus(coupleId);
    }

    public void ensurePeriodReminders(Long coupleId) {
        PeriodSettings s = periodSettingsMapper.findByCoupleId(coupleId);
        if (s == null) {
            return;
        }
        EncData d = safeReadEncData(s);
        if (d == null) {
            return;
        }
        if (d.reminderEnabled != null && !d.reminderEnabled) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate last;
        try {
            last = LocalDate.parse(d.lastStartDate, DTF);
        } catch (Exception e) {
            log.warn("Ignore invalid period lastStartDate for coupleId={}", coupleId);
            return;
        }
        int cycleDays = d.cycleDays == null ? 28 : d.cycleDays;
        int periodDays = d.periodDays == null ? 5 : d.periodDays;
        if (cycleDays < 25 || cycleDays > 45 || periodDays < 3 || periodDays > 7) {
            return;
        }
        LocalDate nextStart = computeNextStart(last, today, cycleDays);
        LocalDate nextEnd = nextStart.plusDays(periodDays - 1L);
        LocalDate ovulation = nextStart.minusDays(14);
        LocalDate fertileStart = ovulation.minusDays(5);
        LocalDate fertileEnd = ovulation.plusDays(4);

        boolean periodLead = !today.isBefore(nextStart.minusDays(3)) && today.isBefore(nextStart);
        boolean ovuLead = !today.isBefore(fertileStart.minusDays(3)) && today.isBefore(fertileStart);
        if (!periodLead && !ovuLead) {
            return;
        }
        Date refDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User u : users) {
            if (u == null || u.getId() == null) {
                continue;
            }
            UserSettings us = userSettingsMapper.findByUserId(u.getId());
            if (us != null && us.getReminderEnabled() != null && us.getReminderEnabled() == 0) {
                continue;
            }
            if (us != null) {
                String ws = us.getReminderWindowStart();
                String we = us.getReminderWindowEnd();
                if (ws != null || we != null) {
                    if (!isInWindow(ws, we)) {
                        continue;
                    }
                }
            }
            if (periodLead) {
                long days = ChronoUnit.DAYS.between(today, nextStart);
                String title = "经期提醒";
                String content = "还有 " + days + " 天 · 预计 " + nextStart.format(DTF) + " 开始（持续 " + periodDays + " 天）";
                notificationService.create(u.getId(), coupleId, NotificationTypes.PERIOD_REMINDER, title, content, coupleId, refDate, true);
            }
            if (ovuLead) {
                long days = ChronoUnit.DAYS.between(today, fertileStart);
                String title = "排卵期提醒";
                String content = "还有 " + days + " 天 · 易孕期预计 " + fertileStart.format(DTF) + " ~ " + fertileEnd.format(DTF) + "（排卵日 " + ovulation.format(DTF) + "）";
                notificationService.create(u.getId(), coupleId, NotificationTypes.OVULATION_REMINDER, title, content, coupleId, refDate, true);
            }
        }
    }

    private PeriodPredictionVO buildPrediction(EncData d) {
        PeriodPredictionVO p = new PeriodPredictionVO();
        int cycleDays = d.cycleDays == null ? 28 : d.cycleDays;
        int periodDays = d.periodDays == null ? 5 : d.periodDays;
        LocalDate last = LocalDate.parse(d.lastStartDate, DTF);
        LocalDate today = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(last, today);
        if (diff < 0) {
            diff = 0;
        }
        int cycleDay = (int) (diff % cycleDays) + 1;
        // 当前周期的起始日（包含 today 的那个周期）
        LocalDate currentStart = last.plusDays((diff / cycleDays) * cycleDays);
        // 当前周期的经期起止日（用于日历着色，让用户看到本月经期信息）
        LocalDate currentPeriodStart = currentStart;
        LocalDate currentPeriodEnd = currentStart.plusDays(periodDays - 1L);

        // 下一次预测经期
        LocalDate nextStart = currentStart.plusDays(cycleDays);
        // 预测日是否已过（用户还没更新 lastStartDate）
        // 判断 lastStartDate + cycleDays 是否已到/已过，而非基于 currentStart 推算
        boolean predictedDatePassed = !today.isBefore(last.plusDays(cycleDays));
        // 用户今天已确认过延迟，当天不再重复提示
        if (predictedDatePassed && d.confirmedDelayDate != null) {
            try {
                LocalDate delayDate = LocalDate.parse(d.confirmedDelayDate, DTF);
                if (!today.isAfter(delayDate)) {
                    predictedDatePassed = false;
                }
            } catch (Exception ignore) { }
        }
        // 计算推迟天数：预测日过了多少天
        Integer daysDelayed = null;
        if (predictedDatePassed) {
            LocalDate predictedStart = last.plusDays(cycleDays);
            daysDelayed = (int) ChronoUnit.DAYS.between(predictedStart, today);
        }

        LocalDate nextEnd = nextStart.plusDays(periodDays - 1L);
        int daysToNext = (int) ChronoUnit.DAYS.between(today, nextStart);
        if (daysToNext < 0) {
            daysToNext = 0;
        }

        p.setCurrentPeriodStart(currentPeriodStart.format(DTF));
        p.setCurrentPeriodEnd(currentPeriodEnd.format(DTF));
        p.setPredictedDatePassed(predictedDatePassed);
        p.setDaysDelayed(daysDelayed);

        LocalDate ovulation = nextStart.minusDays(14);
        LocalDate fertileStart = ovulation.minusDays(5);
        LocalDate fertileEnd = ovulation.plusDays(4);

        LocalDate next2Start = nextStart.plusDays(cycleDays);
        LocalDate next2End = next2Start.plusDays(periodDays - 1L);
        LocalDate ovulation2 = next2Start.minusDays(14);
        LocalDate fertileStart2 = ovulation2.minusDays(5);
        LocalDate fertileEnd2 = ovulation2.plusDays(4);

        p.setCycleDay(cycleDay);
        p.setDaysToNextPeriod(daysToNext);
        p.setNextPeriodStart(nextStart.format(DTF));
        p.setNextPeriodEnd(nextEnd.format(DTF));
        p.setOvulationDay(ovulation.format(DTF));
        p.setFertileStart(fertileStart.format(DTF));
        p.setFertileEnd(fertileEnd.format(DTF));
        p.setNext2PeriodStart(next2Start.format(DTF));
        p.setNext2PeriodEnd(next2End.format(DTF));
        p.setOvulationDay2(ovulation2.format(DTF));
        p.setFertileStart2(fertileStart2.format(DTF));
        p.setFertileEnd2(fertileEnd2.format(DTF));
        p.setPhase(phaseName(today, currentStart, periodDays, ovulation, fertileStart, fertileEnd, nextStart));
        return p;
    }

    private LocalDate computeNextStart(LocalDate last, LocalDate today, int cycleDays) {
        long diff = ChronoUnit.DAYS.between(last, today);
        if (diff < 0) {
            diff = 0;
        }
        LocalDate currentStart = last.plusDays((diff / cycleDays) * cycleDays);
        LocalDate nextStart = currentStart.plusDays(cycleDays);
        if (!nextStart.isAfter(today)) {
            nextStart = nextStart.plusDays(cycleDays);
        }
        return nextStart;
    }

    private String phaseName(LocalDate today,
                             LocalDate currentStart,
                             int periodDays,
                             LocalDate ovulation,
                             LocalDate fertileStart,
                             LocalDate fertileEnd,
                             LocalDate nextStart) {
        LocalDate periodEnd = currentStart.plusDays(periodDays - 1L);
        if (!today.isBefore(currentStart) && !today.isAfter(periodEnd)) {
            return "经期";
        }
        if (!today.isBefore(fertileStart) && !today.isAfter(fertileEnd)) {
            return "排卵期";
        }
        if (today.isAfter(periodEnd) && today.isBefore(fertileStart)) {
            return "卵泡期";
        }
        if (today.isAfter(fertileEnd) && today.isBefore(nextStart)) {
            return "黄体期";
        }
        return "经期";
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

    private EncData readEncData(PeriodSettings s) {
        try {
            String json = cryptoUtil.decrypt(s.getEncData());
            return objectMapper.readValue(json, EncData.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String writeEncData(EncData d) {
        try {
            String json = objectMapper.writeValueAsString(d);
            return cryptoUtil.encrypt(json);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private EncData safeReadEncData(PeriodSettings s) {
        try {
            return readEncData(s);
        } catch (BusinessException e) {
            Long coupleId = s == null ? null : s.getCoupleId();
            log.warn("Ignore unreadable period settings for coupleId={}", coupleId);
            return null;
        }
    }

    private PeriodPredictionVO safeBuildPrediction(EncData d, Long coupleId) {
        try {
            return buildPrediction(d);
        } catch (Exception e) {
            log.warn("Ignore invalid period prediction data for coupleId={}", coupleId);
            return null;
        }
    }

    public static class EncData {
        public Integer cycleDays;
        public Integer periodDays;
        public String lastStartDate;
        public Boolean reminderEnabled;
        public String confirmedDelayDate;
    }
}
