package app.cash.backfila.client.misk.internal

import app.cash.backfila.client.BackfilaApi
import app.cash.backfila.protos.service.ConfigureServiceRequest
import app.cash.backfila.protos.service.ConfigureServiceResponse
import java.io.IOException
import java.io.UncheckedIOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealBackfilaClient @Inject internal constructor(
  private val backfilaApi: BackfilaApi
) : BackfilaClient {
  override fun configureService(request: ConfigureServiceRequest): ConfigureServiceResponse {
    try {
      val response = backfilaApi.configureService(request).execute()
      if (!response.isSuccessful) {
        throw IOException("Call failed: ${response.code()} ${response.message()}")
      }
      return response.body()!!
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
  }
}
