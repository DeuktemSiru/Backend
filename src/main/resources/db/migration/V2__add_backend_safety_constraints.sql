CREATE UNIQUE INDEX IF NOT EXISTS uk_review_consumer_order ON review(consumer_id, order_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_fcm_token_token ON fcm_token(token);
