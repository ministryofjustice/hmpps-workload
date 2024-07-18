package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.apache.logging.log4j.LoggingException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.ContactLoggingMessage

@Service
class ContactLoggingService {
  val log = LoggerFactory.getLogger(this::class.java)
  suspend fun logContact(message: ContactLoggingMessage): Boolean {
    try {
      MDC.put("CRN", message.crn)
      MDC.put("EDIT_NOTES_SCREEN_ACCESSED", message.editNotesScreenAccessed.toString())
      MDC.put("NOTES_EDITED", message.notesEdited.toString())
      log.info("CRN: ${message.crn} allocated, EDIT_NOTES_SCREEN_ACCESSED: ${message.editNotesScreenAccessed}, NOTES_EDITED: ${message.notesEdited}")
      MDC.remove("CRN")
      MDC.remove("EDIT_NOTES_SCREEN_ACCESSED")
      MDC.remove("NOTES_EDITED")
      return true
    } catch (e: LoggingException) {
      log.error(e.message, e)
      return false
    }
  }
}