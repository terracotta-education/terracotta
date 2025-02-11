package edu.iu.terracotta.dao.model.enums;

public enum RegradeOption {

    /**
     * Award points for both corrected and previously correct answers (no scores will be reduced)
     */
    BOTH,

    /**
     * Only award points for the correct answer (some students' scores may be reduced)
     */
    CURRENT,

    /**
     * Give everyone full credit for edited questions
     */
    FULL,

    /**
     * Update edited questions without regrading
     */
    NONE,

    /**
     * Regrade not applicable (default)
     */
    NA

}
