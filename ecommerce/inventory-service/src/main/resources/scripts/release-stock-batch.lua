-- KEYS: list of stock keys
-- ARGV: list of quantities (parallel index)
-- Atomic batch release. Only INCRBY if key already exists — prevents creating
-- a phantom key with no TTL when the cache has already expired, which would
-- cause Redis to permanently drift above the DB value.
for i, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        redis.call('INCRBY', key, tonumber(ARGV[i]))
        redis.call('EXPIRE', key, 3600)
    end
end
return #KEYS
