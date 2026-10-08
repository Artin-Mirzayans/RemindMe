package com.remindme.evals;

// what a feed refresh actually produced. SKIPPED means the data source had nothing to curate,
// so no model call was made - that isn't the AI failing, so it stays out of reliability
public enum GenerationOutcome {
    SUCCESS,
    EMPTY,
    FAILED,
    SKIPPED
}
