package com.kolor.docker.api.entities

import java.security.DigestInputStream

import org.parboiled2._

/**
 * Created by tldeti on 14-12-15.
 */
object Parser {

  case class PortProtocol(port: Int, protocol: Option[String])

  class PortProtocelParser(val input: ParserInput) extends Parser {
    def Number = rule {
      capture(Digits) ~> (_.toInt)
    }

    def Digits = rule {
      oneOrMore(CharPredicate.Digit)
    }

    def PortWithProtocel = rule { Number ~ optional('/' ~ capture("tcp" | "udp")) ~>
      ((num: Int, str: Option[String]) => PortProtocol(num, str)) }

    def InputLine = rule {
      PortWithProtocel ~ EOI
    }
  }


  // TODO write a file path rule
  class VolumeBindParser(val input: ParserInput) extends Parser {
    def Number = rule {
      capture(Digits) ~> (_.toInt)
    }

    def Digits = rule {
      oneOrMore(CharPredicate.Digit)
    }

    def Path = rule {
      oneOrMore(noneOf(":"))
    }

    def RW = rule {
      "ro" | "rw"
    }

    def Binds = rule {
      capture(Path) ~ optional(":" ~ capture(Path)) ~ optional(":" ~ capture(RW)) ~>
        (
          (cP: String, hP: Option[String], rw: Option[String]) =>
            VolumeBind(cP, hP, rw.flatMap(RoERw.apply))
          )
    }

    def InputLine = rule {
      Binds ~ EOI
    }
  }

}
