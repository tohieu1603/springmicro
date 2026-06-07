-- Single-pass: check all keys exist and have sufficient stock, then decrement.
-- Using a two-pass approach risks a key expiring between pass-1 (check) and pass-2
-- (decrement). We first snapshot all values, validate, then apply atomically.
local vals = {}
for i=1,#KEYS do
  local cur = redis.call('GET', KEYS[i])
  if not cur then return -1 end
  if tonumber(cur) < tonumber(ARGV[i]) then return 0 end
  vals[i] = cur
end
-- All checks passed — decrement and refresh TTL (3600 s) to prevent expiry-drift.
for i=1,#KEYS do
  redis.call('DECRBY', KEYS[i], ARGV[i])
  redis.call('EXPIRE', KEYS[i], 3600)
end
return 1
