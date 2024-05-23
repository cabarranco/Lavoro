CREATE TABLE `research.inplay_event_exceptions`
AS SELECT
  '' AS id,
  '' AS eventId,
  '' AS exception_code,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `research.inplay_event_details`
AS SELECT
  '' AS id,
  '' AS eventId,
  CURRENT_TIMESTAMP() AS kickOffTime,
  CURRENT_TIMESTAMP() AS firstHalfEndTime,
  CURRENT_TIMESTAMP() AS secondHalfStartTime,
  CURRENT_TIMESTAMP() AS secondHalfEndTime,
  '' AS scoreFirstHalfEnd,
  '' AS scoreSecondHalfEnd,
  CURRENT_TIMESTAMP() AS createTimestamp,
  '' AS source,
LIMIT 0;

CREATE TABLE `research.betfair_soccer_inplay_1970_staging`
AS SELECT
  '' AS id,
  CURRENT_TIMESTAMP() AS updateTime,
  CURRENT_TIMESTAMP() AS createTime,
  '' AS createCorrelationId
LIMIT 0;

CREATE TABLE `betstore.betfair_soccer_inplay`
AS SELECT
  '' AS id,
  '' AS eventId,
  CURRENT_TIMESTAMP() AS updateTime,
  1 AS matchTime,
  '' AS team,
  '' AS updateType,
  '' AS score,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `betstore.betfair_market_catalogue`
PARTITION by date(startTime)
CLUSTER by eventId, asbSelectionId
OPTIONS(
   description="Betfair markets catalogue",
   require_partition_filter=true
)
AS SELECT
  CURRENT_TIMESTAMP() AS startTime,
  '' AS competition,
  '' AS eventName,
  '' AS eventId,
  '' AS marketName,
  '' AS marketId,
  '' AS runnerName,
  '' AS selectionId,
  '' AS asbSelectionId,
  '' AS id,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `betstore.betfair_historical_data`
PARTITION by date(publishTime)
CLUSTER by eventId, asbSelectionId, publishTime
OPTIONS(
   description="Captured betfair markets back/Lay prices",
   require_partition_filter=true
)
AS SELECT
  '' AS eventId,
  '' AS marketId,
  '' AS asbSelectionId,
  '' AS selectionId,
  '' AS status,
  true AS inplay,
  1.0 AS totalMatched,
  1.0 AS backPrice,
  1.0 AS backSize,
  1.0 AS layPrice,
  1.0 AS laySize,
  CURRENT_TIMESTAMP() AS publishTime,
  '' AS id,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `research.event_prices_analytics`
PARTITION by date(timestamp)
CLUSTER by eventId, asbSelectionId, timestamp
OPTIONS(
   description="Event prices computed analytics",
   require_partition_filter=true
)
AS SELECT
  '' AS id,
  '' AS eventId,
  CURRENT_TIMESTAMP() AS timestamp,
  '' AS asbSelectionId,
  1.0 AS backPrice,
  1.0 AS layPrice,
  1.0 AS backSize,
  1.0 AS laySize,
  1.0 AS spreadPrice,
  1.0 AS deltaBackPrice,
  1.0 AS deltaLayPrice,
  1.0 AS deltaBackSize,
  1.0 AS deltaLaySize,
  1.0 AS deltaSpreadPrice,
  1.0 AS muBackPrice,
  1.0 AS muLayPrice,
  1.0 AS muBackSize,
  1.0 AS muLaySize,
  1.0 AS muSpreadPrice,
  1.0 AS sigmaBackPrice,
  1.0 AS sigmaLayPrice,
  1.0 AS sigmaBackSize,
  1.0 AS sigmaLaySize,
  1.0 AS sigmaSpreadPrice,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `research.event_inplay_features`
PARTITION by date(timestamp)
CLUSTER by eventId, secondsInPlay, timestamp
OPTIONS(
   description="Inplay features for events",
   require_partition_filter=true
)
AS SELECT
    '' AS id,
    '' AS eventId,
    CURRENT_TIMESTAMP() AS timestamp,
    1 AS secondsInPlay,
    1 AS minsToEnd,
    1 AS cumYCardsH,
    1 AS cumRCardsH,
    1 AS cumYCardsA,
    1 AS cumRCardsA,
    1 AS cumGoalsH,
    1 AS cumGoalsA,
    '' AS score,
    '' AS previousScore,
    1.0 AS volumeMO,
    1.0 AS volumeCS,
    1.0 AS volumeOU05,
    1.0 AS volumeOU15,
    1.0 AS volumeOU25,
    1.0 AS volumeOU35,
    1.0 AS volumeAH,
    CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `research.event_prelive_features`
PARTITION by date(timestamp)
CLUSTER by eventId, minsToEnd, timestamp
OPTIONS(
   description="Prelive features for events",
   require_partition_filter=true
)
AS SELECT
    '' AS id,
    '' AS eventId,
    CURRENT_TIMESTAMP() AS timestamp,
    1 AS minsToEnd,
    1.0 AS volumeMO,
    1.0 AS volumeCS,
    1.0 AS volumeOU05,
    1.0 AS volumeOU15,
    1.0 AS volumeOU25,
    1.0 AS volumeOU35,
    1.0 AS volumeAH,
    CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `research.sim_account_balance`
AS SELECT
    CURRENT_DATETIME() AS datetime,
    '' AS currency,
    1.0 AS tradingDayAvailableBalance,
    '' AS node
LIMIT 0;

CREATE TABLE `pulse_reporting.account_balance`
AS SELECT
    CURRENT_DATETIME() AS datetime,
    '' AS username,
    1.0 AS availableToBet,
    '' AS currency,
    1.0 AS balanceSaving,
    1.0 AS tradingDayAvailableBalance,
    '' AS node
LIMIT 0;

CREATE TABLE `pulse_reporting.audit`
AS SELECT
    '' AS opportunityId,
    '' AS marketChangeId,
    '' AS strategyId,
    '' AS marketId,
    '' AS eventId,
    '' AS logEntry,
    CURRENT_TIMESTAMP() AS logEntryTimestamp
LIMIT 0;

CREATE TABLE `research.sim_orders`
AS SELECT
    CURRENT_TIMESTAMP() AS orderTimestamp,
    '' AS venue,
    '' AS bookRunner,
    '' AS marketId,
    '' AS orderSide,
    1.0 AS orderAllocation,
    '' AS orderAllocationCurrency,
    1.0 AS orderPrice,
    '' AS orderType,
    '' AS orderId,
    '' AS selectionId,
    '' AS eventId,
    '' AS opportunityId,
    '' AS strategyId,
    '' AS eventName,
    true AS inPlay,
    '' AS node,
    1.0 AS PL,
LIMIT 0;

CREATE TABLE `pulse_reporting.orders_bets`
AS SELECT
    CURRENT_TIMESTAMP() AS orderTimestamp,
    '' AS venue,
    '' AS orderStatus,
    '' AS bookRunner,
    '' AS marketId,
    '' AS orderSide,
    1.0 AS orderAllocation,
    '' AS orderAllocationCurrency,
    1.0 AS orderPrice,
    '' AS orderType,
    1.0 AS betAmount,
    '' AS betAmountCurrency,
    1.0 AS betPrice,
    '' AS abortReason,
    '' AS betId,
    '' AS selectionId,
    '' AS eventId,
    '' AS opportunityId,
    '' AS strategyId,
    '' AS executionStatus,
    '' AS eventName,
    true AS inPlay,
    '' AS node
LIMIT 0;

CREATE TABLE `pulse_reporting.strategies`
AS SELECT
    '' AS strategyId,
    '' AS allocatorId,
    '' AS hedgeStrategyId,
    true as isActive,
    '' AS json,
    '' AS node,
    current_date() AS betDate
LIMIT 0;

CREATE TABLE `pulse_reporting.strategies_audit`
AS SELECT
    '' AS strategyId,
    '' AS allocatorId,
    '' AS hedgeStrategyId,
    '' AS json,
    '' AS node
LIMIT 0;

CREATE TABLE `betstore.betfair_sofascore_event_mapping`
AS SELECT
    '' AS betfairEventId,
    '' AS sofascoreEventId,
    CURRENT_TIMESTAMP() AS createTimestamp
LIMIT 0;

CREATE TABLE `betstore.betfair_events`
AS SELECT
    '' AS id,
    CURRENT_TIMESTAMP() AS startTime,
    '' AS countryCode,
    '' AS homeTeam,
    '' AS awayTeam,
    '' AS name,
    CURRENT_TIMESTAMP() AS createTimestamp
LIMIT 0;

CREATE TABLE `betstore.betfair_events_mapping_exceptions`
AS SELECT
    '' AS id,
    CURRENT_TIMESTAMP() AS startTime,
    '' AS countryCode,
    '' AS homeTeam,
    '' AS awayTeam,
    '' AS name,
    CURRENT_TIMESTAMP() AS createTimestamp
LIMIT 0;

CREATE TABLE `betstore.sofascore_events`
AS SELECT
    '' AS id,
    CURRENT_TIMESTAMP() AS startTime,
    '' AS countryCode,
    '' AS homeTeam,
    '' AS awayTeam,
    CURRENT_TIMESTAMP() AS createTimestamp
LIMIT 0;

CREATE TABLE `betstore.sofascore_event_incidents`
AS SELECT
    '' AS id,
    '' AS eventId,
    1 AS index,
    1  AS time,
    '' AS incidentClass,
    '' AS incidentType,
    '' AS json,
    CURRENT_TIMESTAMP() AS createTimestamp
LIMIT 0;

CREATE TABLE `betstore.sofascore_soccer_inplay`
AS SELECT
  '' AS id,
  '' AS eventId,
  CURRENT_TIMESTAMP() AS updateTime,
  1 AS matchTime,
  '' AS team,
  '' AS updateType,
  '' AS score,
  CURRENT_TIMESTAMP() AS createTimestamp,
LIMIT 0;

CREATE TABLE `pulse_reporting.strategy_meta`
AS SELECT
    '' AS strategyId,
    '' AS json
LIMIT 0;

CREATE TABLE `research.sim_orders_pl`
AS SELECT
    '' AS id,
    CURRENT_TIMESTAMP() AS createTimestamp,
    CURRENT_TIMESTAMP() AS betTimestamp,
    1.0 AS allocation,
    '' AS winBookRunner,
    '' AS eventId,
    '' AS eventName,
    '' AS score,
    '' AS opportunityId,
    true AS isFirstBet,
    '' AS strategyId,
    1.0 AS PL,
LIMIT 0;