-- ---------------------------------------------------------------
--
-- Cassandra keyspaces and tables creation scripts.
--
-- ---------------------------------------------------------------

-- Keyspace only for source ticks data.
CREATE KEYSPACE IF NOT EXISTS mts_src
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 3
  };

-- Keyspace for all MTS metadata.
CREATE KEYSPACE IF NOT EXISTS mts_meta
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 3
  };

-- Keyspace only for bars calculated from ticks.
CREATE KEYSPACE IF NOT EXISTS mts_bars
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 3
  };

-- Table for all tickers tick data.
drop table mts_src.ticks;

/*
 * PRIMARY KEY ((ticker_id, ddate), ts)
 * The composite partition key is (ticker_id, day)
 * The clustering key is ts - data in partitions sorted by ts
 * ddate - date of year like dd.mm.yyyy
 *
 * ddate populated with  TODATE(now())
 * ts    populated with  toUnixTimestamp(now())
 *
 */
CREATE TABLE mts_src.ticks(
	ticker_id  int,
	ddate      date,
    ts         timestamp,
    bid        double,
    ask        double,
    PRIMARY KEY((ticker_id, ddate),ts)
) WITH CLUSTERING ORDER BY (ts DESC);


truncate mts_src.ticks;

-- typical queries
select count(*) from mts_src.ticks;

select * from mts_src.ticks where ticker_id=4 and ddate='2018-07-13' order by ts desc;

select max(ts) from mts_src.ticks where ticker_id=4 and ddate='2018-07-13';

select max(ddate) from mts_src.ticks where ticker_id=4;

select max(ts) from mts_src.ticks where ticker_id=4;
