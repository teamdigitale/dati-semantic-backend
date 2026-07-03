-- The dashboard stats endpoint joins SEMANTIC_CONTENT_STATS to the "latest harvester run
-- of the year" per repository via LATEST_HARVESTER_RUN_BY_YEAR. The previous definition
-- picked the latest run regardless of status: a run ending UNCHANGED (or FAILURE, RUNNING,
-- NDC_ISSUES_PRESENT) writes no SEMANTIC_CONTENT_STATS rows, yet became the latest run of
-- the year and thus shadowed the last successful run, blanking the whole dashboard to 0.
-- Only SUCCESS runs reach harvest()/saveStats() and therefore write content stats (see
-- DashboardRepo / GET_ALL_STATS_QUERY, which already filter on STATUS = 'SUCCESS'), so
-- restrict the view to them.
CREATE OR REPLACE VIEW LATEST_HARVESTER_RUN_BY_YEAR AS
WITH LATEST_RUN_BY_YEAR AS (SELECT REPOSITORY_ID,
                                   YEAR(STARTED) AS YEAR,
                                   MAX(STARTED)  AS LATEST_STARTED
                            FROM HARVESTER_RUN
                            WHERE STATUS = 'SUCCESS'
                            GROUP BY REPOSITORY_ID, YEAR(STARTED))
SELECT HR.*
FROM HARVESTER_RUN HR
         JOIN LATEST_RUN_BY_YEAR LRBY
              ON HR.REPOSITORY_ID = LRBY.REPOSITORY_ID
                  AND YEAR(HR.STARTED) = LRBY.YEAR
                  AND HR.STARTED = LRBY.LATEST_STARTED
WHERE HR.STATUS = 'SUCCESS';
