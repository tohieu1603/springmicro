-- KEYS[1] = "flashsale:slots:{id}"  ARGV[1] = quantity to reserve
local k = KEYS[1]
local q = tonumber(ARGV[1])
local cur = redis.call('GET', k)
if not cur then return -1 end        -- cache miss → caller seeds
if tonumber(cur) < q then return 0 end
redis.call('DECRBY', k, q)
redis.call('EXPIRE', k, 86400)       -- refresh TTL on every successful reserve
return tonumber(redis.call('GET', k))  -- new remaining
