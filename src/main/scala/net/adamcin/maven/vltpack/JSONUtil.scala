package net.adamcin.maven.vltpack


class CC[T] { def unapply(a:Any):Option[T] = Some(a.asInstanceOf[T]) }
/**
 * Use these objects to match against a parsed json object, as in:
 *
 *  for {
 *    Some(M(map)) <- List(JSON.parseFull(jsonString))
 *    L(languages) = map("languages")
 *    M(language) <- languages
 *    S(name) = language("name")
 *    B(active) = language("is_active")
 *    D(completeness) = language("completeness")
 *  } yield {
 *    (name, active, completeness)
 *  }
 *
 * @version $Id: JSONUtil.java$
 * @author madamcin
 */
object JSONUtil {

  object M extends CC[Map[String, Any]]
  object L extends CC[List[Any]]
  object S extends CC[String]
  object D extends CC[Double]
  object B extends CC[Boolean]

}