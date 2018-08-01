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
drop table mts_meta.tickers;

CREATE TABLE mts_meta.tickers(
	ticker_id      int,
	ticker_code    text,
	ticker_first   text,
	ticker_seconds text,
    PRIMARY KEY((ticker_id, ticker_code,ticker_first))
);

INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (1,'EURUSD','EUR','USD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (2,'AUDUSD','AUD','USD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (3,'GBPUSD','GBP','USD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (4,'NZDUSD','NZD','USD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (5,'EURCHF','EUR','CHF');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (6,'USDCAD','USD','CAD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (7,'USDCHF','USD','CHF');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (8,'EURCAD','EUR','CAD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (9,'GBPAUD','GBP','AUD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (10,'GBPCAD','GBP','CAD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (11,'GBPCHF','GBP','CHF');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (12,'EURGBP','EUR','GBP');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (13,'GBPNZD','GBP','NZD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (14,'NZDCAD','NZD','CAD');

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

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(9,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(10,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(11,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(12,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(13,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(14,600,1);

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
	last_bar_ts_end timestamp,
	deep            int,
	ticker_id       int,
	bar_width_sec   int,
    ts_begin        timestamp,
    ts_end          timestamp,
    o               double,
    h               double,
    l               double,
    c               double,
    h_body          double,
    h_shad          double,
    btype           varchar,
    ticks_cnt       int,
    disp            double,
    PRIMARY KEY((last_bar_ts_end, deep, ticker_id, bar_width_sec),ts_end)
) WITH CLUSTERING ORDER BY (ts_end asc)


-----------------

ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF.

If set WARN =>                WARN , ERROR , FATAL
IF set INFO =>         INFO , WARN , ERROR , FATAL
IF set DEBUG => DEBUG, INFO , WARN , ERROR , FATAL

Use TRACE when you need finer-grained detail than DEBUG can provide, most likely when you
are troubleshooting a particularly difficult problem.

//------------------------------------------------------

// советники по индивидуальным тикерам.
//  ticker_id  - по какому тикеру работает данный советник
//  s_desc     - текстовое описание алгоритма.
//  func_name  - имя функции в scala, пакет bar.adviser class TradeAdviser
//  is_enabled - 0,1 признак включения, выключения советника.
drop TABLE mts_meta.trade_advisers_ticker;
// -------------------------------------------------------
CREATE TABLE mts_meta.trade_advisers_ticker(
	adviser_id     int,
	ticker_id      int,
	s_desc         text,
	func_name      text,
	is_enabled     int,
    PRIMARY KEY(adviser_id,ticker_id, is_enabled, func_name)
);

insert into mts_meta.trade_advisers_ticker(adviser_id,ticker_id,s_desc,func_name,is_enabled)
                                    values(1,1,'Если последние 3 (bar_width_sec = 600 сек. = 10 мин.) бара
                                              EUR растет/падает к доллару (EURUSD) и все прочие тикеры EURXXX имеют сходное движение:
                                              все 3 бара в одну сторону или 2 в одну и один нейтральный, причем нейтральный не последний.
                                              Тогда рекомендуется входить по EURUSD в направлении движения.
                                             ','trd_adv_1_simple',1);

select * from mts_meta.trade_advisers_ticker;


//================================================================
// Результаты работы советника.
CREATE TABLE mts_meta.trade_advisers_results(
	adviser_id int,
    ts         timestamp,
    PRIMARY KEY((adviser_id),ts)
) WITH CLUSTERING ORDER BY (ts DESC);

//для некоторых советников записывается состояние бара(ов) в моммент рекомендации
CREATE TABLE mts_bars.trade_advisers_results_bars(
	adviser_id     int,
    ts             timestamp,
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
    ticks_cnt       int,
    disp           double,
     PRIMARY KEY((adviser_id,ts),ticker_id,ts_end)
) WITH CLUSTERING ORDER BY (ticker_id desc,ts_end DESC);

//================================================================




