CREATE TABLE feature.feature_history (
    id uuid NOT NULL,
    name varchar(100) NOT NULL,
    status varchar(8) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    deleted_at timestamptz NOT NULL,
    deleted_by text NOT NULL,
    CONSTRAINT pk_feature_history PRIMARY KEY (id),
    CONSTRAINT ck_feature_history_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_feature_history_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_feature_history_version CHECK (version >= 1),
    CONSTRAINT ck_feature_history_timestamps CHECK (created_at <= updated_at AND updated_at <= deleted_at),
    CONSTRAINT ck_feature_history_actor CHECK (length(btrim(deleted_by)) > 0)
);
COMMENT ON TABLE feature.feature_history IS 'Immutable snapshot moved from feature when deleted; no foreign key to the removed row';
COMMENT ON COLUMN feature.feature_history.id IS 'Original feature UUID; never reused by application registration';
COMMENT ON COLUMN feature.feature_history.name IS 'Name immediately before deletion';
COMMENT ON COLUMN feature.feature_history.status IS 'Status immediately before deletion';
COMMENT ON COLUMN feature.feature_history.created_at IS 'Original registration instant';
COMMENT ON COLUMN feature.feature_history.updated_at IS 'Last effective change instant before deletion';
COMMENT ON COLUMN feature.feature_history.version IS 'Original optimistic version; not incremented by deletion';
COMMENT ON COLUMN feature.feature_history.deleted_at IS 'Server-assigned deletion instant in UTC';
COMMENT ON COLUMN feature.feature_history.deleted_by IS 'Authenticated actor supplied by a trusted adapter';
CREATE INDEX ix_feature_history_deleted_at_id ON feature.feature_history (deleted_at, id);
