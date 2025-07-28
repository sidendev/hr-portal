package utils.validation

trait Validator {

  def isNotEmpty(fieldName: String, value: String): Option[(String, String)] =
    if (value.trim.isEmpty) Some(fieldName -> s"$fieldName cannot be empty")
    else None

  def isNonBlankIfDefined(fieldName: String, value: Option[String]): Option[(String, String)] =
    value match {
      case Some(v) if v.trim.isEmpty => Some(fieldName -> s"$fieldName cannot be blank if provided")
      case _ => None
    }

  def isValidEmail(fieldName: String, value: String): Option[(String, String)] = {
    val emailRegex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"
    if (!value.matches(emailRegex)) Some(fieldName -> s"$fieldName is not a valid email address")
    else None
  }

  def isPositiveInt(fieldName: String, value: Int): Option[(String, String)] =
    if (value <= 0) Some(fieldName -> s"$fieldName must be a positive number")
    else None
}

