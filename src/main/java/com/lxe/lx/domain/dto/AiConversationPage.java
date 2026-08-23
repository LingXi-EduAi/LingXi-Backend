package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiConversationPage {
    private List<AiConversationSummary> list = new ArrayList<>();
    private int total;
    private int currentPage;
    private int pageSize;
}
