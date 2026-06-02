package com.dz.couple.module.timeline.service;

import com.dz.couple.module.timeline.dto.TimelineItemVO;
import com.dz.couple.module.timeline.mapper.TimelineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TimelineService {
    private final TimelineMapper timelineMapper;

    @Autowired
    public TimelineService(TimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    public List<TimelineItemVO> list(Long userId, Long coupleId, List<String> types, Date fromAt, Date toAt, Date beforeAt, Integer beforeTypeRank, Long beforeId, int limit) {
        return timelineMapper.list(userId, coupleId, types, fromAt, toAt, beforeAt, beforeTypeRank, beforeId, limit);
    }
}

