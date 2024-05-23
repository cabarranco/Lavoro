### Queries repository

FILTER_SELECTIONS = "AND CAST(t1.asbSelectionId AS INT64) IN ({selections_str})"

PRICES_QUERY = """SELECT t1.*
FROM `{DB}.research.event_prices_analytics` t1
WHERE t1.`timestamp` between '{start_time}' and '{end_time}'
{filter_selections}"""

PRICES_SCORES_QUERY = """SELECT t3.interval,
t1.eventId,
t1.asbSelectionId,
t1.backPrice, t1.layPrice, t1.backSize, t1.laySize,
t1.`timestamp`
FROM `{DB}.research.event_prices_analytics` t1
JOIN `{DB}.research.event_inplay_features` t2
ON t2.`timestamp` = t1.`timestamp`
{filter_selections}
AND t1.`timestamp` between '{start_time}' and '{end_time}'
AND t2.`timestamp` between '{start_time}' and '{end_time}'
AND t2.score = '{score}'
AND t1.backPrice > 0
AND t1.layPrice > 0
AND t1.backSize > {size}
AND t1.laySize > {size}
JOIN `{DB}.static.event_time_intervals` t3
ON t2.secondsInPlay BETWEEN t3.downSecondsInPlay AND t3.upSecondsInPlay""" # 12.7GiB

PRICES_FEATURES_QUERY = """SELECT t1.eventId, t1.`timestamp`,
t3.interval,
t1.asbSelectionId, t1.backPrice, t1.layPrice, t1.backSize, t1.laySize,
t2.*
FROM `{DB}.research.event_prices_analytics` t1
JOIN `{DB}.research.event_inplay_features` t2
ON t2.`timestamp` = t1.`timestamp`
{filter_selections}
AND t1.`timestamp` between '{start_time}' and '{end_time}'
AND t2.`timestamp` between '{start_time}' and '{end_time}'
AND t2.score = '{score}'
AND t1.backPrice > 0
AND t1.layPrice > 0
AND t1.backSize > {size}
AND t1.laySize > {size}
JOIN `{DB}.static.event_time_intervals` t3
ON t2.secondsInPlay BETWEEN t3.downSecondsInPlay AND t3.upSecondsInPlay""" # 12.7GiB

PRELIVE_PRICES_QUERY = """SELECT t3.interval,
t1.eventId,
t1.asbSelectionId,
t1.backPrice, t1.layPrice, t1.backSize, t1.laySize,
t1.`timestamp`
FROM `{DB}.research.event_prices_analytics` t1
JOIN `{DB}.research.event_prelive_features` t2
ON t2.`timestamp` = t1.`timestamp`
AND ((
CAST(t1.asbSelectionId AS INT64) = {main_selection}
AND t1.backSize > {backsize}
AND t1.laySize > {laysize}
) OR CAST(t1.asbSelectionId AS INT64) IN ({selections_str})
)
AND t1.`timestamp` between '{start_time}' and '{end_time}'
AND t2.`timestamp` between '{start_time}' and '{end_time}'
AND t1.backPrice > 0
AND t1.layPrice > 0
AND t1.backSize > 0
AND t1.laySize > 0
JOIN `{DB}.static.event_time_intervals` t3
ON t2.minsToEnd BETWEEN t3.downMinsFromEnd AND t3.upMinsFromEnd""" # 12.7GiB
# TODO: change to time difference from start time

RESULT_TIMES_QUERY = """SELECT id eventId, startTime
FROM `{DB}.betstore.betfair_events``
WHERE `startTime` between '{start_time}' and '{end_time}'"""

RESULTS_QUERY = """SELECT betfairEventId eventId, MAX(score) result
FROM `{DB}.betstore.sofascore_soccer_inplay` t
JOIN `{DB}.betstore.betfair_sofascore_event_mapping` m
ON m.sofascoreEventId = t.eventId
GROUP BY betfairEventId"""

COMPETITIONS_QUERY = """SELECT DISTINCT eventId, competition
FROM `{DB}.betstore.betfair_market_catalogue`
WHERE startTime between '{start_time}' and '{end_time}'""" # 33.4MiB

COMPETITIONS_QUERY = """SELECT DISTINCT eventId, competition
FROM `{DB}.betstore.betfair_market_catalogue`
WHERE startTime between '{start_time}' and '{end_time}'""" # 33.4MiB

LIQUIDITY_GROUPS_QUERY = """SELECT b.competition, a.competitionGroup
FROM `{DB}.static.competition_liquidity_groups` a
JOIN (
    SELECT competition, MAX(uploadDate) uploadDate
    FROM `{DB}.static.competition_liquidity_groups`
    GROUP BY competition
) b
ON b.competition = a.competition AND b.uploadDate = a.uploadDate""" # 15.5KiB

OUTCOME_QUERY = """SELECT * FROM `{DB}.static.selection_scores`
WHERE ((side = 'Back' AND asbSelectionId IN ({selections_back}))
OR (side = 'Lay' AND asbSelectionId IN ({selections_lay})))
AND outcome = 'Win'""" # 162KiB

EXTRACT_DATA_FOR_LIQUIDITY_GROUPS_QUERY = """SELECT b.competition, a.eventId, a.totalMatched
FROM `{DB}.betstore.betfair_historical_data` a
join (
    SELECT t0.eventId, t0.competition, t0.startTime, t0.marketId, t0.selectionId,
    max(t1.publishTime) publishTime
    FROM `{DB}.betstore.betfair_market_catalogue` t0
    join `{DB}.betstore.betfair_historical_data` t1
    on t1.eventId = t0.eventId
    and t1.marketId = t0.marketId
    and t1.selectionId = t0.selectionId
    where t0.asbSelectionId = '1' AND t1.inplay = false
    and t0.startTime >= '{start_date}' # rolling current data - interval 2 years
    and t1.publishTime >= '{start_date}'
    and t1.publishTime < t0.startTime
    group by t0.eventId, t0.competition, t0.startTime, t0.marketId, t0.selectionId
) b
on b.eventId = a.eventId and b.marketId = a.marketId and b.selectionId = a.selectionId
and a.publishTime = b.publishTime 
and a.inplay = false
where a.publishTime >= '{start_date}'"""

CREATE_LIQUIDITY_GROUPS_QUERY = """create table if not exists
`{DB}.static.competition_liquidity_groups` (
    uploadDate DATE,
    competition STRING,
    medianVol FLOAT64,
    averageVol FLOAT64,
    minVol FLOAT64,
    maxVol FLOAT64,
    inSampleEvents INT64,
    competitionGroup STRING
)"""

POPULATE_LIQUIDITY_GROUPS_QUERY = """
Insert into `{DB}.static.competition_liquidity_groups`
(uploadDate, competition, medianVol, averageVol, minVol, maxVol, inSampleEvents, competitionGroup) values
"""
