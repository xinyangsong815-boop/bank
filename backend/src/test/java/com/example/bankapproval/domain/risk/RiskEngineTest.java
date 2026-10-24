package com.example.bankapproval.domain.risk;

import com.example.bankapproval.infrastructure.risk.LegacyRiskAdapter;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskEngineTest {
    @Test
    void strategiesAreCombinedAndCapped() {
        RiskEngine engine = new RiskEngine(Arrays.asList(
                new AmountRiskStrategy(), new PurposeKeywordRiskStrategy(), new LegacyRiskAdapter()));
        RiskResult result = engine.evaluate(new RiskContext(new BigDecimal("1500000"), "虚拟币代付", "6222000000000000"));
        assertEquals(100, result.getScore());
        assertEquals("HIGH", result.getLevel());
    }
}
