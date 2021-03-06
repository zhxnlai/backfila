package app.cash.backfila.service

import java.time.Instant
import misk.hibernate.Constraint
import misk.hibernate.Id
import misk.hibernate.Operator
import misk.hibernate.Order
import misk.hibernate.Query

interface RunInstanceQuery : Query<DbRunInstance> {
  @Constraint("backfill_run_id")
  fun backfillRunId(backfillRunId: Id<DbBackfillRun>): RunInstanceQuery

  @Constraint("run_state")
  fun runState(runState: BackfillState): RunInstanceQuery

  @Constraint("lease_expires_at", Operator.LT)
  fun leaseExpiresAtBefore(time: Instant): RunInstanceQuery

  @Order("instance_name")
  fun orderByName(): RunInstanceQuery
}
