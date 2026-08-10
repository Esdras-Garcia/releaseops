CREATE TABLE approval_decisions
(
    id              UUID        PRIMARY KEY,
    review_round_id UUID        NOT NULL,
    reviewer_id     UUID        NOT NULL,
    decision_type   VARCHAR(32) NOT NULL,
    comment         TEXT,
    decided_at      TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_approval_decision_round
        FOREIGN KEY (review_round_id)
            REFERENCES review_rounds (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_approval_decision_reviewer
        UNIQUE (review_round_id, reviewer_id),

    CONSTRAINT ck_approval_decision_type
        CHECK (decision_type IN ('APPROVED', 'REJECTED')),

    CONSTRAINT ck_approval_decision_comment
        CHECK (
            (comment IS NULL OR length(trim(comment)) > 0)
                AND
            (decision_type <> 'REJECTED' OR comment IS NOT NULL)
            )
);

CREATE INDEX idx_approval_decision_round
    ON approval_decisions (review_round_id);