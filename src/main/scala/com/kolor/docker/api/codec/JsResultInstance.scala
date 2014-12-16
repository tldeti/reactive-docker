package com.kolor.docker.api.codec

import play.api.libs.json.{JsError, JsSuccess, JsResult}

import scalaz._

/**
 * Created by tldeti on 14-12-15.
 */
object JsResultInstance {
  implicit val JsResultApplicative = new Applicative[JsResult] {
    override def point[A](a: => A): JsResult[A] =
      JsSuccess(a)

    override def ap[A, B](fa: => JsResult[A])(f: => JsResult[(A) => B]): JsResult[B] =
      (f, fa) match {
        case (JsSuccess(f, _), JsSuccess(a, _)) => JsSuccess(f(a))
        case (JsError(e1), JsError(e2)) => JsError(JsError.merge(e1, e2))
        case (JsError(e), _) => JsError(e)
        case (_, JsError(e)) => JsError(e)
      }
  }
}
