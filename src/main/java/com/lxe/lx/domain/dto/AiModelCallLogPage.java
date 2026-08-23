package com.lxe.lx.domain.dto;

import com.lxe.lx.pojo.AiModelCallLog;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiModelCallLogPage {
    private List<AiModelCallLog> list = new ArrayList<>();
    private int total;
    private int currentPage;
    private int pageSize;
    private long totalTokens;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private long averageLatencyMs;
    private long failedCount;
}
