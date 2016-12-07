package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.RootsEnvironment;

public final class TestAnomalyDetector extends AnomalyDetector {

    private final AnomalyDetectorFunction function;
    private final long waitDuration;
    
    private TestAnomalyDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder);
        this.function = builder.function;
        this.waitDuration = builder.waitDuration;
    }

    @Override
    public void run(long now) {
        if (function != null) {
            function.run(now, this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static class Builder extends AnomalyDetectorBuilder<TestAnomalyDetector,Builder> {
        
        private AnomalyDetectorFunction function;
        private long waitDuration = -1L;
        
        private Builder() {
        }

        public Builder setFunction(AnomalyDetectorFunction function) {
            this.function = function;
            return this;
        }

        public Builder setWaitDuration(long waitDuration) {
            this.waitDuration = waitDuration;
            return this;
        }

        @Override
        public TestAnomalyDetector build(RootsEnvironment environment) {
            return new TestAnomalyDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
    
    public interface AnomalyDetectorFunction {
        void run(long now, TestAnomalyDetector detector);
    }
}
