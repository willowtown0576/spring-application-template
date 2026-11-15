ALTER TABLE feature.feature
    ADD COLUMN status varchar(8) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version bigint NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_feature_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    ADD CONSTRAINT ck_feature_version CHECK (version >= 1),
    ADD CONSTRAINT ck_feature_timestamps CHECK (updated_at >= created_at);

CREATE INDEX ix_feature_name_id ON feature.feature (name, id);
CREATE INDEX ix_feature_updated_at_id ON feature.feature (updated_at, id);

COMMENT ON COLUMN feature.feature.status IS 'ACTIVE or INACTIVE; independent of deletion';
COMMENT ON COLUMN feature.feature.created_at IS 'Server-assigned registration instant in UTC';
COMMENT ON COLUMN feature.feature.updated_at IS 'Last effective change instant in UTC';
COMMENT ON COLUMN feature.feature.version IS 'Optimistic version starting at 1; incremented for effective changes';

CREATE TABLE feature.registration_request (
    actor text NOT NULL,
    request_id uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    result_ids uuid[] NOT NULL DEFAULT '{}',
    CONSTRAINT pk_registration_request PRIMARY KEY (actor, request_id)
);
COMMENT ON TABLE feature.registration_request IS 'Atomic registration replay results; retain keys while retries must be deduplicated';
COMMENT ON COLUMN feature.registration_request.actor IS 'Authenticated actor provided by a trusted adapter';
COMMENT ON COLUMN feature.registration_request.request_id IS 'Registration operation key; reused for retries';
COMMENT ON COLUMN feature.registration_request.payload_hash IS 'SHA-256 of ordered registration values';
COMMENT ON COLUMN feature.registration_request.result_ids IS 'Created IDs in input order, committed with the registrations';
