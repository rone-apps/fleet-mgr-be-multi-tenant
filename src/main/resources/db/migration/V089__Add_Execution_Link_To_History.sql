-- V089__Add_Execution_Link_To_History.sql
-- Adds execution_id column to statement_balance_transfer_history table
-- to link history records to their execution (in addition to config)

ALTER TABLE statement_balance_transfer_history
ADD COLUMN execution_id BIGINT,
ADD CONSTRAINT fk_history_execution FOREIGN KEY (execution_id)
    REFERENCES statement_transfer_execution(id) ON DELETE SET NULL;

CREATE INDEX idx_history_execution ON statement_balance_transfer_history(execution_id);

-- Rollback:
-- ALTER TABLE statement_balance_transfer_history DROP FOREIGN KEY fk_history_execution;
-- ALTER TABLE statement_balance_transfer_history DROP INDEX idx_history_execution;
-- ALTER TABLE statement_balance_transfer_history DROP COLUMN execution_id;
