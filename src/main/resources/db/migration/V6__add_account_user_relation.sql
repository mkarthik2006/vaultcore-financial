ALTER TABLE accounts
    ADD COLUMN user_id UUID;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id);