package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier

@Converter
class TierConverter : AttributeConverter<Tier, String> {
  override fun convertToDatabaseColumn(tier: Tier?): String {
    if (tier == null) {
      throw IllegalArgumentException("Tier cannot be null")
    }

    return tier.name
  }

  override fun convertToEntityAttribute(value: String?): Tier {
    if (value == null) {
      return Tier.MISSING
    }

    return try {
      Tier.valueOf(value)
    } catch (_: IllegalArgumentException) {
      Tier.MISSING
    }
  }
}
