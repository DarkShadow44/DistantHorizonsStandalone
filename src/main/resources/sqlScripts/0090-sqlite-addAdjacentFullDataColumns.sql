
-- storing adjacent data (IE a single line of data on the +X/-X/+Z/-Z axis)
-- allows for significantly reduced render loading times since we only have to
-- handle part of the adjacent data source vs all of it

alter table FullData add column NorthAdjData BLOB NULL;
--batch--
alter table FullData add column SouthAdjData BLOB NULL;
--batch--
alter table FullData add column EastAdjData BLOB NULL;
--batch--
alter table FullData add column WestAdjData BLOB NULL;
--batch--