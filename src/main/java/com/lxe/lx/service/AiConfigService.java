package com.lxe.lx.service;

import com.lxe.lx.pojo.AiConfig;

import java.util.List;

/**
 * Dify application / workflow version configuration store.
 * Supports "change config -> switch -> rollback" without hard-coding values in code.
 */
public interface AiConfigService {

    /** All config rows (every version) for an environment, newest version first. */
    List<AiConfig> listAll(String env);

    /** Only the currently effective (active) config rows for an environment. */
    List<AiConfig> listActive(String env);

    /** The currently effective value for a single key, or null if none. */
    AiConfig getActive(String configKey, String env);

    /**
     * Create a new version for a key WITHOUT activating it.
     * Returns the created row.
     */
    AiConfig createVersion(String configKey, String configValue, String env, String remark);

    /**
     * Create a new version for a key AND activate it immediately (change + switch).
     * Returns the newly active row.
     */
    AiConfig updateValue(String configKey, String configValue, String env, String remark);

    /** Enable (activate) a specific version row by id; deactivates the previous active row. */
    AiConfig enableVersion(String id);

    /** Roll back to a historical version of a key; deactivates the current active row. */
    AiConfig rollback(String configKey, String env, int version);
}
