package edu.ucsb.cs.roots.utils;

public final class CommandOutput {

    private final int status;
    private final String stdout;
    private final String stderr;

    public CommandOutput(int status, String stdout, String stderr) {
        this.status = status;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getStatus() {
        return status;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    @Override
    public String toString() {
        return "Process terminated with status: " + status + "; output: " + stderr
                + "; error: " + stderr;
    }
}
