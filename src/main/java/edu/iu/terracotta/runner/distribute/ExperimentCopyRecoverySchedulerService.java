package edu.iu.terracotta.runner.distribute;

public interface ExperimentCopyRecoverySchedulerService {

    /**
     * Restarts every copied-experiment recreation that stopped part-way through (e.g. the server
     * was restarted while it ran). Returns how many destination courses were restarted.
     */
    int recover();

}
