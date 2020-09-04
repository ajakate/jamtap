CREATE TABLE comments
(id SERIAL PRIMARY KEY,
creator VARCHAR(100),
content VARCHAR(100),
commented_at TIMESTAMP,
track_id INT references tracks(id));
