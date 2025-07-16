package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Document {
    private String id;
    private String name;
    private String type;
    private String description;
    private String fileAddress;
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;
}
