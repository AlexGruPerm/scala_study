-- ---------------------------------------------------------------
--
-- Cassandra keyspaces and tables creation scripts.
--
-- ---------------------------------------------------------------

-- Keyspace only for source ticks data.
CREATE KEYSPACE IF NOT EXISTS mts_src
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
  };

-- Keyspace for all MTS metadata.
CREATE KEYSPACE IF NOT EXISTS mts_meta
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
  };

-- Keyspace only for bars calculated from ticks.
CREATE KEYSPACE IF NOT EXISTS mts_bars
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
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

CREATE INDEX ticks_ticker ON mts_src.ticks(ticker_id); // for search max ddate by ticker
//Ex: select max(ddate) from mts_src.ticks where ticker_id=1;

//==================================================================================================================

/*
 * Table for store calculated bars for each ticker, each bar type (bar_width_sec = 30,60,90... seconds)
 * ts_begin - timestamp when bar begin (open time)
 * ts_end   - timestamp when bar end (close time)
 * o,h,l,c  - bar common properties
 * h_body, h_shad - bar hights, body and full, shadow
 * btype text code, upbar - g (green), o=c - n(none), o>c downbar - r (red) : g,n,r
 *
 */
CREATE TABLE mts_bars.bars(
	ticker_id      int,
	ddate          date,
	bar_width_sec  int,
    ts_begin       timestamp,
    ts_end         timestamp,
    o              double,
    h              double,
    l              double,
    c              double,
    h_body         double,
    h_shad         double,
    btype          varchar,
    PRIMARY KEY((ticker_id, ddate, bar_width_sec),ts_begin)
) WITH CLUSTERING ORDER BY (ts_begin DESC);


select * from mts_bars.bars;

//==================================================================================================================

/*
 * Tickers dictionary.
 */
CREATE TABLE mts_meta.tickers(
	ticker_id      int,
	ticker_code    text,
    PRIMARY KEY(ticker_id, ticker_code)
);

insert into mts_meta.tickers(ticker_id,ticker_code) values(1,'EURUSD');
insert into mts_meta.tickers(ticker_id,ticker_code) values(2,'AUDUSD');
insert into mts_meta.tickers(ticker_id,ticker_code) values(3,'GBPUSD');
insert into mts_meta.tickers(ticker_id,ticker_code) values(4,'NZDUSD');
insert into mts_meta.tickers(ticker_id,ticker_code) values(5,'EURCHF');
insert into mts_meta.tickers(ticker_id,ticker_code) values(6,'USDCAD');
insert into mts_meta.tickers(ticker_id,ticker_code) values(7,'USDCHF');
insert into mts_meta.tickers(ticker_id,ticker_code) values(8,'EURCAD');

select * from mts_meta.tickers;

/*
 * Table with meta properties each sizes used for each ticker for
 * bars calculator.
 */
CREATE TABLE mts_meta.bars_property(
	ticker_id      int,
	bar_width_sec  int,
	is_enabled     int,
    PRIMARY KEY(ticker_id, bar_width_sec, is_enabled)
);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,30,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,60,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,60,1);

select * from mts_meta.bars_property where ticker_id=1;

//==================================================================================================================