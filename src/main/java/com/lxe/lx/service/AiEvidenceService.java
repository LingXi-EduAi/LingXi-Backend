package com.lxe.lx.service;

import com.lxe.lx.pojo.AiEvidence;

import java.util.List;

public interface AiEvidenceService {
    void saveAll(String messageId, List<AiEvidence> evidences);

    List<AiEvidence> getByMessageId(String messageId);
}
