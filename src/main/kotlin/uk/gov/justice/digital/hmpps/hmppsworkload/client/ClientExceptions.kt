package uk.gov.justice.digital.hmpps.hmppsworkload.client

class WorkloadFailedDependencyException(message: String) : RuntimeException(message)

class WorkloadWebClientTimeoutException(message: String) : RuntimeException(message)
