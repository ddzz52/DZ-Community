package com.dz.couple.module.timeline.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.timeline.dto.TimelineItemVO;
import com.dz.couple.module.timeline.service.TimelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/timeline")
@Validated
public class TimelineController {
    private static final SimpleDateFormat DT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> TYPE_SET = new HashSet<String>();

    static {
        TYPE_SET.add("DIARY");
        TYPE_SET.add("PHOTO");
        TYPE_SET.add("ANNIVERSARY");
        TYPE_SET.add("WISH_DONE");
        TYPE_SET.add("SCRATCH_DONE");
    }

    private final TimelineService timelineService;

    @Autowired
    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping
    public ApiResponse<List<TimelineItemVO>> list(@RequestParam(value = "types", required = false) String types,
                                                  @RequestParam(value = "from", required = false) String from,
                                                  @RequestParam(value = "to", required = false) String to,
                                                  @RequestParam(value = "beforeAt", required = false) String beforeAt,
                                                  @RequestParam(value = "beforeTypeRank", required = false) Integer beforeTypeRank,
                                                  @RequestParam(value = "beforeId", required = false) Long beforeId,
                                                  @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 30 : Math.min(Math.max(limit, 10), 80);
        Date f = parseFlexible(from, false);
        Date t = parseFlexible(to, true);
        Date ba = parseFlexible(beforeAt, false);
        Integer btr = beforeTypeRank;
        Long bid = beforeId;
        if (ba != null) {
            if (btr == null || bid == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "beforeAt/beforeTypeRank/beforeId 需要同时传");
            }
        }
        List<String> typeList = parseTypes(types);
        return ApiResponse.ok(timelineService.list(userId, coupleId, typeList, f, t, ba, btr, bid, l));
    }

    private List<String> parseTypes(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String[] parts = s.split(",");
        List<String> out = new ArrayList<String>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim().toUpperCase();
            if (t.isEmpty()) continue;
            if (!TYPE_SET.contains(t)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "types 不合法");
            }
            if (!out.contains(t)) out.add(t);
        }
        return out.isEmpty() ? null : out;
    }

    private Date parseFlexible(String s, boolean endOfDay) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String v = s.trim();
        if (v.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            v = v + (endOfDay ? " 23:59:59" : " 00:00:00");
        }
        try {
            return DT.parse(v);
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期格式需为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
