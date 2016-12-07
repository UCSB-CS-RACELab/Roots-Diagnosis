package edu.ucsb.cs.roots.changepoint;

import edu.ucsb.cs.roots.rlang.RService;

import static com.google.common.base.Preconditions.checkArgument;

public final class CustomPELTChangePointDetector extends PELTChangePointDetector {

    private final double penaltyValue;

    public CustomPELTChangePointDetector(RService rService, double penaltyValue) {
        super(rService);
        checkArgument(penaltyValue > 0, "Penalty value must be positive");
        this.penaltyValue = penaltyValue;
    }

    @Override
    protected String getRCall() {
        return String.format("cpt.mean(x, method='PELT', penalty='Manual', pen.value='%f')",
                penaltyValue);
    }
}
