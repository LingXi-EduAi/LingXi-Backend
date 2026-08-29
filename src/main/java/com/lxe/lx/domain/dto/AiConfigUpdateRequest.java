package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiConfigUpdateRequest {
    private String configKey;
    private String configValue;
    private String env;
    private String remark;
}
