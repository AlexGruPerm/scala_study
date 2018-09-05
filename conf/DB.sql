-- ---------------------------------------------------------------
--
-- Cassandra keyspaces and tables creation scripts.
--
-- ---------------------------------------------------------------

to elemenate messgaes
Out of 28 commit log syncs over the past 142,06s with average duration of 47,92ms, 1 have exceeded the configured commit interval by an average of 393,31ms
need change
commitlog_sync: periodic commitlog_sync_period_in_ms: 10000

/**

Commit log settings

commitlog_sync
(Default: periodic) The method that Cassandra uses to acknowledge writes in milliseconds:
periodic: (Default: 10000 milliseconds [10 seconds])
With commitlog_sync_period_in_ms, controls how often the commit log is synchronized to disk. Periodic syncs are acknowledged immediately.

batch: (Default: disabled)note
Used with commitlog_sync_batch_window_in_ms (Default: 2 ms), which is the maximum length of time that queries may be batched together.

*/

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


drop table mts_src.ticks;

/*
 * PRIMARY KEY ((ticker_id, ddate), ts)
 * The composite partition key is (ticker_id, day)
 *
 *
 */
CREATE TABLE mts_src.ticks(
	ticker_id  int,
	ddate      date,     //Trade servser - date from TimeCurrent
	ts         bigint,   //Trade servser - 64 bits, Date and time with second precision, encoded as 8 bytes since epoch.
	db_tsunx   bigint,   //db layer - calculated as toUnixTimestamp(now())
    bid        double,
    ask        double,
    PRIMARY KEY((ticker_id, ddate),ts,db_tsunx)
) WITH CLUSTERING ORDER BY (ts DESC,db_tsunx DESC);

truncate mts_src.ticks;

select
	ticker_id  ,
	ddate      ,
    ts         ,
    srv_time,
    db_tsunx   ,
    db_ts      ,
    bid        ,
    ask
from mts_src.ticks;

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
    ts_begin       bigint,
    ts_end         bigint,
    o              double,
    h              double,
    l              double,
    c              double,
    h_body         double,
    h_shad         double,
    btype          varchar,
    ticks_cnt      int,
    disp           double,
    log_co         double,
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
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (15,'AUDCAD','AUD','CAD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (16,'AUDCHF','AUD','CHF');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (17,'AUDJPY','AUD','JPY');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (18,'CADJPY','CAD','JPY');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (19,'CHFJPY','CHF','JPY');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (20,'EURNZD','EUR','NZD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (21,'EURJPY','EUR','JPY');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (22,'USDJPY','USD','JPY');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (23,'USDRUB','USD','RUB');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (24,'GBPSGD','GBP','SGD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (25,'USDSGD','USD','SGD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (26,'XAUUSD','XAU','USD');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (27,'_BRN','_BRN','XXX');
INSERT INTO mts_meta.tickers (ticker_id,ticker_code,ticker_first,ticker_seconds) VALUES (28,'_DXY','_DXY','XXX');

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



select * from mts_meta.bars_property;

truncate  mts_meta.bars_property;

insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(1,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(2,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(3,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(4,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(5,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(6,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(7,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(8,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(9,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(9,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(9,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(10,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(10,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(10,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(11,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(11,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(11,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(12,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(12,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(12,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(13,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(13,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(13,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(14,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(14,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(14,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(15,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(15,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(15,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(16,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(16,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(16,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(17,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(17,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(17,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(18,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(18,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(18,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(19,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(19,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(19,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(20,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(20,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(20,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(21,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(21,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(21,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(22,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(22,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(22,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(23,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(23,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(23,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(24,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(24,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(24,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(25,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(25,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(25,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(26,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(26,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(26,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(27,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(27,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(27,600,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(28,30,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(28,300,1);
insert into mts_meta.bars_property(ticker_id,bar_width_sec,is_enabled) values(28,600,1);


select * from mts_meta.bars_property where ticker_id=1;

//==================================================================================================================

CREATE TABLE mts_bars.lastbars(
	ticker_id      int,
	bar_width_sec  int,
    ts_begin       bigint,
    ts_end         bigint,
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
)


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
    ts         bigint,
    main_way   text,
    PRIMARY KEY((adviser_id),ts)
) WITH CLUSTERING ORDER BY (ts DESC)


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


/*
  Table for results of Tend adviser (n hours), must be divided on 3 parts
  with equal size in seconds:
  barsList.sliding(6,6).toList = 3 parts.
  ---------------------------------------------------------------------
   For example: width_sec  = 600 sec.
                l_deep_sec = 3*3600 sec.
                3*3600/600 = 18 bars / 3 = 6 bars in each of 3 parts.
  ---------------------------------------------------------------------
  Look "TendAdviser" in code.
 */
CREATE TABLE mts_meta.way_adviser_n_hours(
	ticker_id        int,
	bar_width_sec    int,
	ts_res           bigint,
    way              text,
    deep_sec         int,
    adv_bars_in_part int,
    -------------------
    p1_size_bars   int,
    p2_size_bars   int,
    p3_size_bars   int,
    -------------------
    p1_cnt         int,
    p2_cnt         int,
    p3_cnt         int,
    -------------------
    p1_logco       double,
    p2_logco       double,
    p3_logco       double,
 PRIMARY KEY((ticker_id, bar_width_sec),ts_res)
) WITH CLUSTERING ORDER BY (ts_res DESC);

truncate mts_meta.way_adviser_n_hours;

select * from mts_meta.way_adviser_n_hours;


drop table mts_meta.bar_price_distrib;

truncate mts_meta.bar_price_distrib;

/*
 Distribution bars by prices
 */
CREATE TABLE mts_meta.bar_price_distrib(
	ticker_id        int,
	bar_width_sec    int,
	price            double,
	cnt              int,
 PRIMARY KEY((ticker_id, bar_width_sec),price)
) WITH CLUSTERING ORDER BY (price ASC);

select * from mts_meta.bar_price_distrib;


//==========================================================


/*
 Хранится анализируемый тик: ticker_id, bar_width_sec, ts_end
 и данные его анализа из будущего,
 для дальнейшей работы с паттернами и ранжированием баров.
 ln(exit) - ln(enter) =
  0.0017 ~ 20 b.p. eurusd
  0.0034 ~ 40
  0.0051 ~ 60
*/
CREATE TABLE mts_bars.bars_future(
	ticker_id      int,
	bar_width_sec  int,
    ts_end         bigint,
    c              double,
    ft_log_0017_ts_end     bigint,
    ft_log_0017_res        varchar,//u,d
    ft_log_0017_cls_price  double,
    ft_log_0034_ts_end     bigint,
    ft_log_0034_res        varchar,//u,d
    ft_log_0034_cls_price  double,
    ft_log_0051_ts_end     bigint,
    ft_log_0051_res        varchar,//u,d
    ft_log_0051_cls_price  double,
    PRIMARY KEY((ticker_id, bar_width_sec),ts_end)
) WITH CLUSTERING ORDER BY (ts_end DESC);

truncate mts_bars.bars_future;

select count(*) from mts_bars.bars_future;

select * from mts_bars.bars_future where ticker_id=1 and bar_width_sec=30 order by ts_end limit 5000;


//****************************

CREATE TABLE mts_bars.pattern_search_results(
	ticker_id            int,
	bar_width_sec        int,
    patt_ts_begin        bigint,       // ts_begin первого    бара в искомой (текущей) формации
    patt_ts_end          bigint,       // ts_end   последнего бара в искомой формации
    patt_bars_count      int,          // количество баров в искомой формации
    history_found_tsends list<bigint>, // List(ts_end) найденных формаций в истории по искомой формации
    //--------------------------
    ft_log_0017_res_u        int,      //количество найденных u для уровня 0.0017 в mts_bars.bars_future для всех history_found_tsends
    ft_log_0017_res_d        int,
    ft_log_0017_res_n        int,
    //--------------------------
    ft_log_0034_res_u        int,
    ft_log_0034_res_d        int,
    ft_log_0034_res_n        int,
    //--------------------------
    ft_log_0051_res_u        int,
    ft_log_0051_res_d        int,
    ft_log_0051_res_n        int,
    //--------------------------
    ft_log_sum_u             int,
    ft_log_sum_d             int,
    ft_log_sum_n             int,
    PRIMARY KEY((ticker_id, bar_width_sec),patt_ts_end)
) WITH CLUSTERING ORDER BY (patt_ts_end DESC);














