package uk.gov.justice.digital.hmpps.hmppsworkload

@SpringBootApplication(
  exclude = [
    // exclude anything that drags in WebClient/SQS/etc.
    org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration::class,
    org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration::class,
    org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration::class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration::class
  ]
)
class SchemaMigrator {
}