-- KEYS[1] = idempotency key, ARGV[1] = state token ("PROCESSING"), ARGV[2] = TTL seconds
-- Returns: "new" if claimed, existing state otherwise
local existing = redis.call('GET', KEYS[1])
if existing then return existing end
redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
return "new"
