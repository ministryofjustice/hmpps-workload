package uk.gov.justice.digital.hmpps.hmppsworkload.domain.event

data class ContactLoggingMessage(val crn: String, val editNotesScreenAccessed: Boolean, val notesEdited: Boolean)