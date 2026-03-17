
-- This is done to fix a bug where a lot of unnecessary
-- ID mapping data is saved, which significantly reduces
-- loading/deserializing/decompression time


-- delete all data above 0 (max detail)
-- so it can be re-created
delete from FullData where DetailLevel > 0;
--batch--

-- re-downsample all LOD data
update FullData set ApplyToParent = 1;
