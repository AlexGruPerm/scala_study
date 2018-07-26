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
    ticks_cnt      int,    -- тиковая плотность
    disp           double, -- среднеквадратичное отклонение тиков в баре от Мо.
    PRIMARY KEY((ticker_id, ddate, bar_width_sec),ts_end)
) WITH CLUSTERING ORDER BY (ts_end DESC);



 insert into mts_bars.bars(
 	ticker_id,
	ddate,
	bar_width_sec,
    ts_begin,
    ts_end,
    o,
    h,
    l,
    c,
    h_body,
    h_shad,
    btype
 ) values(
 	2,
 	'2018-07-17',
 	30,
 	1531804871493,
 	1531804891493,
 	1.23,
 	2.34,
 	3.35,
 	4.36,
 	3.12,
 	4.15,
 	'g'
 );



 select
	ticker_id,
	ddate,
	bar_width_sec,
    ts_begin,
    ts_end,
    o,
    h,
    l,
    c,
    h_body,
    h_shad,
    btype,
    toUnixTimestamp(ts_end) as ts_end_unx
 from mts_bars.bars;

//==================================================================================================================

/*
 * Tickers dictionary.
 */
CREATE TABLE mts_meta.tickers(
	ticker_id      int,
	ticker_code    text,
    PRIMARY KEY(ticker_id, ticker_code)
);

INSERT INTO tickers (ticker_id,ticker_code) VALUES (1,'EURUSD');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (2,'AUDUSD');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (3,'GBPUSD');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (4,'NZDUSD');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (5,'EURCHF');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (6,'USDCAD');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (7,'USDCHF');
INSERT INTO tickers (ticker_id,ticker_code) VALUES (8,'EURCAD');

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
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,600,1);

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,90,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,600,1);


select * from mts_meta.bars_property where ticker_id=1;

//==================================================================================================================

CREATE TABLE mts_bars.lastbars(
	ticker_id      int,
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
    ticks_cnt       int,
    disp           double,
    PRIMARY KEY((ticker_id, bar_width_sec))
);


select * from mts_meta.tickers;

select count(*) from mts_src.ticks;

select * from mts_bars.bars;

select * from mts_bars.lastbars where bar_width_sec=30 allow filtering;

select * from mts_bars.lastbars where bar_width_sec=90 allow filtering;

select * from mts_bars.lastbars where bar_width_sec=600 allow filtering;


truncate mts_src.ticks;
truncate mts_bars.bars;
truncate mts_bars.lastbars;


--задает для каждого тикера+бар(ширина) сколкьо последних баров хранить,
-- для одного тикера+бар(ширина) может быть задано одно значение.
CREATE TABLE mts_meta.bars_property_last_deeps(
	ticker_id      int,
	bar_width_sec  int,
	is_enabled     int,
	deep_count     int,
    PRIMARY KEY(ticker_id, bar_width_sec, is_enabled, deep_count)
);

-- для тиекра 1 и баров с шириной 30 секунд, хранить последние 10 баров в таблице - mts_bars.lastNbars
insert into mts_meta.bars_property_last_deeps(ticker_id,bar_width_sec,is_enabled,deep_count) values(1,30,1,10);

CREATE TABLE mts_bars.lastNbars(
	deep           int,
	ticker_id      int,
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
    ticks_cnt      int,
    disp           double,
    PRIMARY KEY((deep, ticker_id, bar_width_sec))
);


-----------------

ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF.

If set WARN =>                WARN , ERROR , FATAL
IF set INFO =>         INFO , WARN , ERROR , FATAL
IF set DEBUG => DEBUG, INFO , WARN , ERROR , FATAL

Use TRACE when you need finer-grained detail than DEBUG can provide, most likely when you
are troubleshooting a particularly difficult problem.









