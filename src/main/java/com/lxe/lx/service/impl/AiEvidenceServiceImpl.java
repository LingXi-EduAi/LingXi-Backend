package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiEvidenceMapper;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.service.AiEvidenceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiEvidenceServiceImpl implements AiEvidenceService {
    private final AiEvidenceMapper evidenceMapper;

    public AiEvidenceServiceImpl(AiEvidenceMapper evidenceMapper) {
        this.evidenceMapper = evidenceMapper;
    }

    @Override
    public void saveAll(String messageId, List<AiEvidence> evidences) {
        if (messageId == null || evidences == null) {
            return;
        }
        for (AiEvidence evidence : evidences) {
            if (evidence == null) {
                continue;
            }
            evidence.setMessageId(messageId);
            evidenceMapper.insert(evidence);
        }
    }

    @Override
    public List<AiEvidence> getByMessageId(String messageId) {
        return evidenceMapper.findByMessageId(messageId);
    }
}
