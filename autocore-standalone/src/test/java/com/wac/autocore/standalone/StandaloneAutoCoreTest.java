package com.wac.autocore.standalone;

import com.wac.autocore.headless.StepResult;
import com.wac.autocore.security.Role;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StandaloneAutoCoreTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleRunsAsExpected(Role role) {
        List<StepResult> results = new StandaloneAutoCore(role).runScenario();

        assertTrue(results.stream().allMatch(StepResult::isAsExpected),
                () -> role + " had steps that did not go as expected: " + results.stream()
                        .filter(r -> !r.isAsExpected()).map(StepResult::getName).toList());
    }
}
