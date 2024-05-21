package uk.gov.justice.digital.hmpps.hmppsworkload.config

import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.trace.Span
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private const val BEARER = "Bearer "
private const val CLIENT_ID = "client_id"

class CallingClientWebFilter : WebFilter {
  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
    val auth = exchange.request.headers.get(HttpHeaders.AUTHORIZATION)
    val predicate: (String) -> Boolean = { it.startsWith(BEARER) }
    val claims = auth?.first(predicate)?.substringAfter(BEARER)
    if (claims != null) {
      Span.current().setAttribute(
        CLIENT_ID,
        SignedJWT.parse(claims).jwtClaimsSet
          .getClaim(CLIENT_ID).toString(),
      )
    } else {
      Span.current().setAttribute(
        CLIENT_ID,
        "No Client id provided",
      )
    }
    return chain.filter(exchange)
  }
}
