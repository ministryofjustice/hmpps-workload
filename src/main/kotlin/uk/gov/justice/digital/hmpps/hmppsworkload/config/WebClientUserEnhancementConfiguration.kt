package uk.gov.justice.digital.hmpps.hmppsworkload.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient

@Configuration
class WebClientUserEnhancementConfiguration(
  @Value("\${hmpps-tier.endpoint.url}") private val hmppsTierApiRootUri: String,
  @Value("\${assess-risks-needs.endpoint.url}") private val assessRisksNeedsApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
) {
  private fun assessRisksNeedsWebClient(builder: WebClient.Builder, uri: String): WebClient = builder.baseUrl(assessRisksNeedsApiRootUri)
    .filter(withAuth())
    .build()

  @Bean
  fun hmppsTierWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManagerUserEnhanced(clientRegistrationRepository, builder), builder, hmppsTierApiRootUri, "hmpps-tier-api")

  @Bean
  @Qualifier("assessRisksNeedsClientUserEnhancedAppScope")
  fun assessRisksNeedsClientUserEnhancedAppScope(
    builder: WebClient.Builder,
  ): WebClient = assessRisksNeedsWebClient(builder, assessRisksNeedsApiRootUri)

  @Bean
  @Qualifier("assessRisksNeedsClientUserEnhanced")
  fun assessRisksNeedsClientUserEnhanced(@Qualifier("assessRisksNeedsClientUserEnhancedAppScope") webClient: WebClient): AssessRisksNeedsApiClient = AssessRisksNeedsApiClient(webClient)

  @Primary
  @Bean
  fun hmppsTierApiClientUserEnhanced(@Qualifier("hmppsTierWebClientUserEnhancedAppScope") webClient: WebClient): HmppsTierApiClient = HmppsTierApiClient(webClient)

  private fun authorizedClientManagerUserEnhanced(clients: ReactiveClientRegistrationRepository?, builder: WebClient.Builder): ReactiveOAuth2AuthorizedClientManager {
    val service: ReactiveOAuth2AuthorizedClientService = InMemoryReactiveOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, service)
    val reactiveClientCredentialsTokenResponseClient = WebClientReactiveClientCredentialsTokenResponseClient()

    reactiveClientCredentialsTokenResponseClient.setWebClient(builder.filter(userEnhancedTokenRequestProcessor()).build())

    val reactiveAuthorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
      .builder()
      .clientCredentials { reactiveClientCredentialsGrantBuilder: ReactiveOAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        reactiveClientCredentialsGrantBuilder.accessTokenResponseClient(reactiveClientCredentialsTokenResponseClient)
      }.build()

    manager.setAuthorizedClientProvider(reactiveAuthorizedClientProvider)
    return manager
  }

  fun userEnhancedTokenRequestProcessor(): ExchangeFilterFunction = ExchangeFilterFunction.ofRequestProcessor { request ->
    ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .map(Authentication::getName)
      .map { username ->
        val builder = ClientRequest.from(request)
        val body = request.body()
        if (body is BodyInserters.FormInserter<*>) {
          @Suppress("UNCHECKED_CAST")
          builder.body((body as BodyInserters.FormInserter<String>).with("username", username))
        }
        builder.build()
      }
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

  @Bean
  fun workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManagerUserEnhanced(clientRegistrationRepository, builder), builder, workforceAllocationsToDeliusApiRootUri, "workforce-allocations-to-delius-api")

  @Bean
  fun workforceAllocationsToDeliusApiClientUserEnhanced(@Qualifier("workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope") webClient: WebClient): WorkforceAllocationsToDeliusApiClient = WorkforceAllocationsToDeliusApiClient(webClient)

  private fun withAuth(): ExchangeFilterFunction = ExchangeFilterFunction.ofRequestProcessor { request ->
    ReactiveSecurityContextHolder.getContext()
      .map { securityContext ->
        val authentication = securityContext.authentication
        val token = when (authentication) {
          is BearerTokenAuthentication -> authentication.token.tokenValue
          is OAuth2AccessToken -> authentication.tokenValue
          is JwtAuthenticationToken -> authentication.token.tokenValue
          else -> null
        }
        token?.let {
          ClientRequest.from(request)
            .header("Authorization", "Bearer $it")
            .build()
        } ?: request
      }
  }
}
