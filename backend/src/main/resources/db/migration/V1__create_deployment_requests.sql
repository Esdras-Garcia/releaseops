CREATE TABLE deployment_requests
(
    id                  UUID         PRIMARY KEY,
    requester_id        UUID         NOT NULL,
    release_id          UUID         NOT NULL,
    environment_id      UUID         NOT NULL,
    title               VARCHAR(120) NOT NULL,
    description         VARCHAR(5000) NOT NULL,
    rollback_plan       VARCHAR(5000) NOT NULL,
    required_approvals  INTEGER      NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    cancellation_reason TEXT,
    canceled_at         TIMESTAMPTZ,

    CONSTRAINT ck_deployment_required_approvals
        CHECK (required_approvals > 0),

    CONSTRAINT ck_deployment_status
        CHECK (status IN (
                          'DRAFT',
                          'PENDING_APPROVAL',
                          'APPROVED',
                          'CANCELED'
            )),

    CONSTRAINT ck_deployment_title
        CHECK (length(trim(title)) > 0),

    CONSTRAINT ck_deployment_description
        CHECK (length(trim(description)) > 0),

    CONSTRAINT ck_deployment_rollback_plan
        CHECK (length(trim(rollback_plan)) > 0),

    CONSTRAINT ck_deployment_cancellation
        CHECK (
            (
                status = 'CANCELED'
                    AND cancellation_reason IS NOT NULL
                    AND canceled_at IS NOT NULL
                )
                OR
            (
                status <> 'CANCELED'
                    AND cancellation_reason IS NULL
                    AND canceled_at IS NULL
                )
            )
);