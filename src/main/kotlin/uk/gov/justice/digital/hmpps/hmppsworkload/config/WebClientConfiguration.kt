package uk.gov.justice.digital.hmpps.hmppsworkload.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.FeatureFlagClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps-tier.endpoint.url}") private val hmppsTierApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
  @Value("\${assess-risks-needs.endpoint.url}") private val assessRisksNeedsApiRootUri: String,
  @Value("\${feature-flag.endpoint.url}") private val featureFlagApiRootUri: String,
) {

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    oAuth2AuthorizedClientService: ReactiveOAuth2AuthorizedClientService,
  ): ReactiveOAuth2AuthorizedClientManager {
    val authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun assessRisksNeedsApiWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, assessRisksNeedsApiRootUri, "assess-risks-needs-api")

  @Bean
  fun assessRiskNeedsApiClient(@Qualifier(value = "assessRisksNeedsApiWebClientAppScope") webClient: WebClient): AssessRisksNeedsApiClient = AssessRisksNeedsApiClient(webClient)

  @Bean
  fun workforceAllocationsToDeliusApiWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, workforceAllocationsToDeliusApiRootUri, "workforce-allocations-to-delius-api")

  @Bean
  fun workforceAllocationsToDeliusApiClient(@Qualifier("workforceAllocationsToDeliusApiWebClientAppScope") webClient: WebClient): WorkforceAllocationsToDeliusApiClient = WorkforceAllocationsToDeliusApiClient(webClient)

  @Bean
  fun hmppsTierWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, hmppsTierApiRootUri, "hmpps-tier-api")

  @Bean
  fun hmppsTierApiClient(@Qualifier("hmppsTierWebClientAppScope") webClient: WebClient): HmppsTierApiClient = HmppsTierApiClient(webClient)

  @Bean
  fun featureFlagClient(builder: WebClient.Builder): FeatureFlagClient {
    val rootUri = featureFlagApiRootUri
    val apiKey = System.getenv("FLIPT_API_KEY")
    return FeatureFlagClient(getFliptWebClient(builder, rootUri, apiKey))
  }

  private fun getOAuthWebClient(
    authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String,
  ): WebClient {
    val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .filter(oauth2Client)
      .build()
  }

  private fun getFliptWebClient(
    builder: WebClient.Builder,
    rootUri: String,
    apiKey: String,
  ): WebClient = builder
    .baseUrl(rootUri)
    .defaultHeader(
      "Authorization",
      "Bearer $apiKey",
    )
    .build()
}
