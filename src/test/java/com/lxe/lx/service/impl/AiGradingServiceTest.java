package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.service.HomeworkSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGradingServiceTest {

    private DifyGateway difyGateway;
    private HomeworkSubmissionService homeworkSubmissionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        difyGateway = mock(DifyGateway.class);
        homeworkSubmissionService = mock(HomeworkSubmissionService.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void doesNothingWhenDisabled() {
        AiGradingServiceImpl service = new AiGradingServiceImpl(difyGateway, homeworkSubmissionService, false);
        HomeworkSubmission submission = submission();

        service.grade(submission);

        verify(difyGateway, never()).runWorkflow(anyMap(), anyString());
        verify(homeworkSubmissionService, never()).gradeHomework(any());
    }

    @Test
    void gradesSubmissionWhenEnabledAndWorkflowSucceeds() throws Exception {
        AiGradingServiceImpl service = new AiGradingServiceImpl(difyGateway, homeworkSubmissionService, true);
        HomeworkSubmission submission = submission();
        HomeworkSubmission latest = submission();
        latest.setVersion(2);
        when(homeworkSubmissionService.getById("submission-1")).thenReturn(latest);
        when(difyGateway.runWorkflow(anyMap(), anyString())).thenReturn(objectMapper.readTree(
                "{\"data\":{\"outputs\":{\"grade\":95,\"feedback\":\"解答正确，步骤清晰\"}}}"
        ));

        service.grade(submission);

        verify(difyGateway).runWorkflow(anyMap(), anyString());
        assertEquals(Integer.valueOf(95), latest.getGrade());
        assertEquals("解答正确，步骤清晰", latest.getFeedback());
        verify(homeworkSubmissionService).gradeHomework(latest);
    }

    @Test
    void swallowsDifyFailureAndLeavesSubmissionUntouched() throws Exception {
        AiGradingServiceImpl service = new AiGradingServiceImpl(difyGateway, homeworkSubmissionService, true);
        HomeworkSubmission submission = submission();
        when(difyGateway.runWorkflow(anyMap(), anyString()))
                .thenThrow(new RuntimeException("Dify 服务不可用"));

        service.grade(submission);

        verify(homeworkSubmissionService, never()).gradeHomework(any());
        assertNull(submission.getGrade());
        assertNull(submission.getFeedback());
    }

    @Test
    void skipsUpdateWhenWorkflowOutputHasNoValidGrade() throws Exception {
        AiGradingServiceImpl service = new AiGradingServiceImpl(difyGateway, homeworkSubmissionService, true);
        HomeworkSubmission submission = submission();
        when(difyGateway.runWorkflow(anyMap(), anyString())).thenReturn(objectMapper.readTree(
                "{\"data\":{\"outputs\":{\"feedback\":\"无法评分\"}}}"
        ));

        service.grade(submission);

        verify(homeworkSubmissionService, never()).gradeHomework(any());
    }

    @Test
    void skipsWhenSubmissionRecordNoLongerExists() throws Exception {
        AiGradingServiceImpl service = new AiGradingServiceImpl(difyGateway, homeworkSubmissionService, true);
        HomeworkSubmission submission = submission();
        when(homeworkSubmissionService.getById("submission-1")).thenReturn(null);
        when(difyGateway.runWorkflow(anyMap(), anyString())).thenReturn(objectMapper.readTree(
                "{\"data\":{\"outputs\":{\"grade\":90,\"feedback\":\"good\"}}}"
        ));

        service.grade(submission);

        verify(homeworkSubmissionService, never()).gradeHomework(any());
    }

    private HomeworkSubmission submission() {
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId("submission-1");
        submission.setAssignmentId("assignment-1");
        submission.setStudentId("student-1");
        submission.setContent("1+1=2");
        submission.setVersion(1);
        return submission;
    }
}