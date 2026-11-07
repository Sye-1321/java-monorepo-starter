CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    lines BIGINT NOT NULL CHECK (lines >= 0),
    words BIGINT NOT NULL CHECK (words >= 0),
    code_points BIGINT NOT NULL CHECK (code_points >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
