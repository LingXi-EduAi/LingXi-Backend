package com.lxe.lx.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AiTaskStatus {
    public static final String CREATED = "CREATED";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String STOPPED = "STOPPED";
    public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    private static final Set<String> TERMINAL = new HashSet<>(Arrays.asList(
            SUCCEEDED, FAILED, STOPPED, PARTIAL_SUCCESS
    ));

    private AiTaskStatus() {
    }

    public static boolean isTerminal(String status) {
        return TERMINAL.contains(status);
    }

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null || from.equals(to) || isTerminal(from)) {
            return false;
        }
        if (CREATED.equals(from)) {
            return RUNNING.equals(to) || FAILED.equals(to) || STOPPED.equals(to);
        }
        return RUNNING.equals(from) && isTerminal(to);
    }
}
