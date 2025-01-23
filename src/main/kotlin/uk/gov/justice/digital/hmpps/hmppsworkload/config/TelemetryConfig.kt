package uk.gov.justice.digital.hmpps.hmppsworkload.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelemetryConfig {

  @Bean
  fun getTelemetryClient(): TelemetryClient = TelemetryClient()
}
