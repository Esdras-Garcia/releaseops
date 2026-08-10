CREATE TABLE review_rounds
(
    id                    UUID        PRIMARY KEY,
    deployment_request_id UUID        NOT NULL,
    round_number          INTEGER     NOT NULL,
    submitted_at          TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_review_round_deployment
        FOREIGN KEY (deployment_request_id)
            REFERENCES deployment_requests (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_review_round_number
        UNIQUE (deployment_request_id, round_number),

    CONSTRAINT ck_review_round_number
        CHECK (round_number > 0)
);

CREATE INDEX idx_review_round_deployment
    ON review_rounds (deployment_request_id);