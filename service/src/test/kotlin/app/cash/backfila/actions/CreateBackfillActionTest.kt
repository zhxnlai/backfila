package app.cash.backfila.actions

import app.cash.backfila.BackfilaTestingModule
import app.cash.backfila.api.ConfigureServiceAction
import app.cash.backfila.client.Connectors
import app.cash.backfila.dashboard.CreateBackfillAction
import app.cash.backfila.dashboard.CreateBackfillRequest
import app.cash.backfila.fakeCaller
import app.cash.backfila.protos.service.ConfigureServiceRequest
import app.cash.backfila.service.BackfilaDb
import app.cash.backfila.service.BackfillRunQuery
import app.cash.backfila.service.BackfillState
import app.cash.backfila.service.RunInstanceQuery
import com.google.inject.Module
import javax.inject.Inject
import kotlin.test.assertNotNull
import misk.exceptions.BadRequestException
import misk.hibernate.Query
import misk.hibernate.Transacter
import misk.hibernate.newQuery
import misk.scope.ActionScope
import misk.testing.MiskTest
import misk.testing.MiskTestModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@MiskTest(startService = true)
class CreateBackfillActionTest {
  @Suppress("unused")
  @MiskTestModule
  val module: Module = BackfilaTestingModule()

  @Inject lateinit var configureServiceAction: ConfigureServiceAction
  @Inject lateinit var createBackfillAction: CreateBackfillAction
  @Inject lateinit var scope: ActionScope
  @Inject lateinit var queryFactory: Query.Factory
  @Inject @BackfilaDb lateinit var transacter: Transacter

  @Test
  fun serviceDoesntExist() {
    scope.fakeCaller(user = "molly") {
      assertThatThrownBy {
        createBackfillAction.create("deep-fryer", CreateBackfillRequest("abc"))
      }.isInstanceOf(BadRequestException::class.java)
    }
  }

  @Test
  fun backfillDoesntExist() {
    scope.fakeCaller(service = "deep-fryer") {
      configureServiceAction.configureService(
          ConfigureServiceRequest.Builder()
              .connector_type(Connectors.ENVOY)
              .build())
    }

    scope.fakeCaller(user = "molly") {
      assertThatThrownBy {
        createBackfillAction.create("deep-fryer", CreateBackfillRequest("abc"))
      }.isInstanceOf(BadRequestException::class.java)
    }
  }

  @Test
  fun created() {
    scope.fakeCaller(service = "deep-fryer") {
      configureServiceAction.configureService(ConfigureServiceRequest.Builder()
          .backfills(listOf(
              ConfigureServiceRequest.BackfillData("ChickenSandwich", "Description", listOf(), null,
                  null, false)))
          .connector_type(Connectors.ENVOY)
          .build())
    }
    scope.fakeCaller(user = "molly") {
      val response = createBackfillAction.create("deep-fryer",
          CreateBackfillRequest("ChickenSandwich"))

      transacter.transaction { session ->
        val run = queryFactory.newQuery<BackfillRunQuery>().uniqueResult(session)
        assertNotNull(run)
        assertThat(run.state).isEqualTo(BackfillState.PAUSED)
        assertThat(run.created_by_user).isEqualTo("molly")
        assertThat(run.approved_by_user).isNull()
        assertThat(run.approved_at).isNull()
        assertThat(response.id).isEqualTo(run.id.id)

        val instances = queryFactory.newQuery<RunInstanceQuery>()
            .backfillRunId(run.id)
            .orderByName()
            .list(session)
        assertThat(instances).hasSize(2)
        assertThat(instances[0].instance_name).isEqualTo("-80")
        assertThat(instances[0].lease_token).isNull()
        assertThat(instances[0].run_state).isEqualTo(BackfillState.PAUSED)
        assertThat(instances[1].instance_name).isEqualTo("80-")
        assertThat(instances[1].lease_token).isNull()
        assertThat(instances[1].run_state).isEqualTo(BackfillState.PAUSED)
      }
    }
  }
}
