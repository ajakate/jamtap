-- :name get-active-tracks :? :*
-- :doc gets the active tracks
select id, name, started_at, creator
from tracks
where finished_at is null
order by started_at desc

-- :name get-old-tracks :? :*
-- :doc gets the old/finished tracks
select id, name, started_at, finished_at, creator
from tracks
where finished_at is not null
order by started_at desc

-- :name get-track :? :1
-- :doc gets a track
select * from tracks where id = :id

-- :name create-track! :<! :1
-- :doc creates a new track record
INSERT into tracks
(name, started_at, creator)
values (:name, :started_at, :creator)
RETURNING *;

-- :name delete-track! :<! :1
-- :doc deletes a track record
DELETE FROM tracks t
WHERE t.id = :id;

-- :name finish-track! :<! :1
-- :doc finishes a track
UPDATE tracks
set finished_at = :finished_at
where id = :id
RETURNING *;

-- :name create-comment! :<! :1
-- :doc creates a new comment record
INSERT into comments
(creator, content, commented_at, track_id)
values (:creator, :content, :commented_at, :track_id)
RETURNING *;

-- :name get-comments-for-track :? :*
-- :doc gets comments for a track id
select * from comments where
track_id = :track_id
order by commented_at desc;
