package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiConfigMapper;
import com.lxe.lx.pojo.AiConfig;
import com.lxe.lx.service.AiConfigService;
import com.lxe.lx.service.AiTaskApiException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConfigServiceImplTests {

    private final AiConfigMapper mapper = mock(AiConfigMapper.class);
    private final AiConfigService service = new AiConfigServiceImpl(mapper);

    private AiConfig row(String id, String key, String value, String env, boolean active, int version) {
        AiConfig config = new AiConfig();
        config.setId(id);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setEnv(env);
        config.setActive(active);
        config.setVersion(version);
        return config;
    }

    @Test
    void listAllDelegatesToMapper() {
        AiConfig a = row("1", "chatflow.app_id", "app-1", "prod", true, 2);
        AiConfig b = row("2", "chatflow.app_id", "app-0", "prod", false, 1);
        when(mapper.findAll("prod")).thenReturn(Arrays.asList(a, b));

        List<AiConfig> result = service.listAll("prod");

        assertEquals(2, result.size());
        verify(mapper).findAll("prod");
    }

    @Test
    void listActiveReturnsOnlyActiveRows() {
        AiConfig active = row("1", "workflow.app_id", "wf-2", "prod", true, 2);
        AiConfig inactive = row("2", "workflow.app_id", "wf-1", "prod", false, 1);
        when(mapper.findAll("prod")).thenReturn(Arrays.asList(active, inactive));

        List<AiConfig> result = service.listActive("prod");

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    void getActiveReturnsNullWhenNoActiveRow() {
        when(mapper.findActive("chatflow.app_id", "prod")).thenReturn(null);

        assertNull(service.getActive("chatflow.app_id", "prod"));
    }

    @Test
    void createVersionAssignsNextVersionAndStaysInactive() {
        when(mapper.maxVersion("chatflow.app_id", "prod")).thenReturn(2);

        AiConfig created = service.createVersion("chatflow.app_id", "app-3", "prod", "bump");

        assertNotNull(created.getId());
        assertEquals(3, created.getVersion());
        assertEquals(Boolean.FALSE, created.getActive());
        verify(mapper).insert(created);
    }

    @Test
    void createVersionStartsAtVersionOneWhenNoHistory() {
        when(mapper.maxVersion("chatflow.app_id", "prod")).thenReturn(0);

        AiConfig created = service.createVersion("chatflow.app_id", "app-1", "prod", null);

        assertEquals(1, created.getVersion());
    }

    @Test
    void updateValueCreatesNewVersionAndActivatesIt() {
        when(mapper.maxVersion("workflow.prompt_version", "prod")).thenReturn(1);
        AiConfig created = new AiConfig();
        created.setId("new-id");
        created.setConfigKey("workflow.prompt_version");
        created.setConfigValue("v2");
        created.setEnv("prod");
        created.setActive(true);
        created.setVersion(2);
        when(mapper.insert(any(AiConfig.class))).thenAnswer(inv -> {
            AiConfig arg = inv.getArgument(0);
            created.setId(arg.getId());
            created.setVersion(arg.getVersion());
            created.setActive(arg.getActive());
            return 1;
        });
        when(mapper.findActive("workflow.prompt_version", "prod")).thenReturn(created);

        AiConfig result = service.updateValue("workflow.prompt_version", "v2", "prod", "upgrade");

        verify(mapper).deactivateKey("workflow.prompt_version", "prod");
        assertEquals(Boolean.TRUE, result.getActive());
        assertEquals(2, result.getVersion());
    }

    @Test
    void enableVersionActivatesTargetAndDeactivatesKey() {
        AiConfig target = row("target-id", "chatflow.app_id", "app-9", "prod", false, 9);
        when(mapper.findById("target-id")).thenReturn(target);
        when(mapper.activate("target-id")).thenReturn(1);
        when(mapper.findActive("chatflow.app_id", "prod")).thenReturn(target);

        AiConfig result = service.enableVersion("target-id");

        verify(mapper).deactivateKey("chatflow.app_id", "prod");
        verify(mapper).activate("target-id");
        assertEquals("target-id", result.getId());
    }

    @Test
    void enableVersionThrowsWhenRowMissing() {
        when(mapper.findById("missing")).thenReturn(null);

        assertThrows(AiTaskApiException.class, () -> service.enableVersion("missing"));
        verify(mapper, never()).activate(any());
    }

    @Test
    void rollbackReactivatesHistoricalVersion() {
        AiConfig historical = row("hist-id", "workflow.app_id", "wf-1", "prod", false, 1);
        when(mapper.findByKey("workflow.app_id", "prod"))
                .thenReturn(Collections.singletonList(historical));
        when(mapper.activate("hist-id")).thenReturn(1);
        when(mapper.findActive("workflow.app_id", "prod")).thenReturn(historical);

        AiConfig result = service.rollback("workflow.app_id", "prod", 1);

        verify(mapper).deactivateKey("workflow.app_id", "prod");
        verify(mapper).activate("hist-id");
        assertEquals("hist-id", result.getId());
    }

    @Test
    void rollbackThrowsWhenVersionNotFound() {
        when(mapper.findByKey("workflow.app_id", "prod")).thenReturn(Collections.emptyList());

        assertThrows(AiTaskApiException.class,
                () -> service.rollback("workflow.app_id", "prod", 99));
        verify(mapper, never()).activate(any());
    }

    @Test
    void rollbackThrowsWhenVersionAlreadyActive() {
        AiConfig active = row("active-id", "workflow.app_id", "wf-2", "prod", true, 2);
        when(mapper.findByKey("workflow.app_id", "prod"))
                .thenReturn(Collections.singletonList(active));

        assertThrows(AiTaskApiException.class,
                () -> service.rollback("workflow.app_id", "prod", 2));
        verify(mapper, never()).activate(any());
    }

    @Test
    void createVersionRejectsBlankKey() {
        assertThrows(AiTaskApiException.class,
                () -> service.createVersion("  ", "v", "prod", null));
        verify(mapper, never()).insert(any());
    }

    @Test
    void createVersionRejectsBlankValue() {
        assertThrows(AiTaskApiException.class,
                () -> service.createVersion("chatflow.app_id", "  ", "prod", null));
        verify(mapper, never()).insert(any());
    }

    @Test
    void enableVersionThrowsWhenRowNotOwnedByEnv() {
        AiConfig otherEnv = row("x", "chatflow.app_id", "v", "dev", false, 1);
        when(mapper.findById("x")).thenReturn(otherEnv);

        assertThrows(AiTaskApiException.class, () -> service.enableVersion("x"));
    }

    @Test
    void getActiveReturnsRowWhenPresent() {
        AiConfig active = row("1", "chatflow.app_id", "app-1", "prod", true, 1);
        when(mapper.findActive("chatflow.app_id", "prod")).thenReturn(active);

        AiConfig result = service.getActive("chatflow.app_id", "prod");

        assertEquals("app-1", result.getConfigValue());
        assertTrue(result.getActive());
    }
}
