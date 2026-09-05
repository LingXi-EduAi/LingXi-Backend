package com.lxe.lx.service.impl;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic task decomposition utility for BE-04.
 * <p>
 * Splits a user query on Chinese conjunctions ("并且", "然后", standalone "并")
 * into 2-3 subtask goals. If no splitting point is found, keeps the original query
 * as a single subtask. The decomposition is purely string-based and deterministic.
 * <p>
 * Each returned {@link SubtaskSpec} carries the sub-query, agentType (same as taskType),
 * 1-based executionNo, and dependencyJson referencing the previous subtask by index placeholder.
 * <p>
 * <strong>Intermediate limitation:</strong> this class only splits the query string.
 * The actual Dify execution pipeline still runs once for the first subtask; full
 * per-subtask sequential execution is planned for a later iteration.
 */
final class TaskDecomposer {

    private TaskDecomposer() {
    }

    /**
     * Specification for one subtask to be inserted into ai_subtask.
     */
    static final class SubtaskSpec {
        private final String goal;
        private final String agentType;
        private final int executionNo;

        SubtaskSpec(String goal, String agentType, int executionNo) {
            this.goal = goal;
            this.agentType = agentType;
            this.executionNo = executionNo;
        }

        String getGoal() {
            return goal;
        }

        String getAgentType() {
            return agentType;
        }

        int getExecutionNo() {
            return executionNo;
        }
    }

    /**
     * Decompose a query into subtask specs.
     *
     * @param query    the user's natural-language query
     * @param taskType CHATFLOW or WORKFLOW — used as agentType for each subtask
     * @return 1-N SubtaskSpec objects; never empty
     */
    static List<SubtaskSpec> decompose(String query, String taskType) {
        List<String> parts = splitQuery(query);
        List<SubtaskSpec> specs = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            specs.add(new SubtaskSpec(parts.get(i), taskType, i + 1));
        }
        return specs;
    }

    /**
     * Split a Chinese query on conjunctions.
     * <p>
     * Strategy (deterministic, no NLP):
     * <ol>
     *   <li>Split on "并且" (unambiguous conjunction meaning "and/moreover")</li>
     *   <li>Split each resulting segment on "然后" (unambiguous "then")</li>
     *   <li>If still a single segment, attempt split on standalone "并" (conjunction "and")
     *       — but only if the segment is long enough (>4 chars) to reduce false positives
     *       on words like "并非", "并列"</li>
     * </ol>
     *
     * @param query the raw query
     * @return 1-N trimmed, non-blank segments
     */
    static List<String> splitQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.singletonList(StringUtils.defaultString(query, ""));
        }

        // Phase 1: split on "并且" (most unambiguous conjunction)
        String[] phase1 = query.split("并且", -1);
        List<String> result = new ArrayList<>();
        for (String segment : phase1) {
            if (StringUtils.isBlank(segment)) {
                continue;
            }
            // Phase 2: further split each segment on "然后"
            String[] phase2 = segment.split("然后", -1);
            for (String sub : phase2) {
                String trimmed = sub.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }

        // Phase 3: if still a single segment, try standalone "并" as conjunction
        if (result.size() == 1) {
            String single = result.get(0);
            if (single.length() > 4 && single.contains("并")) {
                // Split on "并" that is NOT the first character (guards against "并非" at start)
                // Use a lookbehind: require at least 2 chars before "并"
                String[] phase3 = single.split("(?<=.{2})并", 2);
                if (phase3.length == 2
                        && StringUtils.isNotBlank(phase3[0])
                        && StringUtils.isNotBlank(phase3[1])) {
                    result.clear();
                    result.add(phase3[0].trim());
                    result.add(phase3[1].trim());
                }
            }
        }

        return result.isEmpty() ? Collections.singletonList(query) : result;
    }
}
