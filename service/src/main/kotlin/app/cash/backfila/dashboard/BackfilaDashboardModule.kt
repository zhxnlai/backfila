package app.cash.backfila.dashboard

import javax.inject.Qualifier
import misk.environment.Environment
import misk.inject.KAbstractModule
import misk.web.dashboard.WebTabResourceModule

class BackfilaDashboardModule(val environment: Environment) : KAbstractModule() {
  override fun configure() {
    install(WebTabResourceModule(
      environment = environment,
      slug = "app",
      web_proxy_url = "http://localhost:4200/",
      url_path_prefix = "/app/",
      resourcePath = "classpath:/web/app/"
    ))
  }
}

/** Dashboard Annotation used for all tabs bound in the Backfila App */
@Qualifier
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
annotation class BackfilaApp
