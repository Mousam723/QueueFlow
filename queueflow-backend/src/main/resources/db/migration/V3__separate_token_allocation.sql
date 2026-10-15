ALTER TABLE queues
    ADD COLUMN next_token_number INT NOT NULL DEFAULT 0;
