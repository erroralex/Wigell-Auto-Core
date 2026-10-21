package com.wac.autocore.unit;

import com.wac.autocore.model.WorkOrderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>WorkOrderTypeTest</b>
 * <p>Ansvar: Bevisar vilka ordertyper som kräver problembeskrivning. En ny typ hamnar
 * automatiskt bland dem som inte kräver det, så ett ändrat krav måste beslutas medvetet.</p>
 */
class WorkOrderTypeTest {

    @ParameterizedTest(name = "{0} requires problem description")
    @EnumSource(value = WorkOrderType.class, names = {"DROP_IN", "WARRANTY"})
    void requiresProblemDescription_trueForUnplannedWork(WorkOrderType type) {
        assertTrue(type.requiresProblemDescription(), type + " should require a problem description");
    }

    @ParameterizedTest(name = "{0} does not require problem description")
    @EnumSource(value = WorkOrderType.class, names = {"DROP_IN", "WARRANTY"},
            mode = EnumSource.Mode.EXCLUDE)
    void requiresProblemDescription_falseForOtherTypes(WorkOrderType type) {
        assertFalse(type.requiresProblemDescription(), type + " should not require a problem description");
    }
}
