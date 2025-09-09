package utils

import java.text.Normalizer

object EmailGen {
  private val Domain = "example.com"

  private def slug(s: String): String = {
    val nfd = Normalizer.normalize(s, Normalizer.Form.NFD)
    val noAccents = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    noAccents.toLowerCase.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
  }

  def baseLocal(first: String, last: String): String = {
    val f = slug(first); val l = slug(last)
    if (f.isEmpty && l.isEmpty) "user"
    else if (f.isEmpty) l
    else if (l.isEmpty) f
    else s"$f.$l"
  }

  def candidate(localBase: String, n: Int): String =
    if (n <= 0) s"$localBase@$Domain" else s"$localBase$n@$Domain"
}

