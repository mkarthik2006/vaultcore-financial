-- Composite index for the monthly-statement range scan
-- (findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc) and balance-up-to reads, which filter on
-- account_id together with created_at. The existing single-column idx_ledger_entries_account_id
-- only covers the leading column, so range/ordering still required a sort.
CREATE INDEX idx_ledger_entries_account_created
    ON ledger_entries (account_id, created_at);
