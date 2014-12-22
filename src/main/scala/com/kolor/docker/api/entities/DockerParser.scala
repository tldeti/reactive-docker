package com.kolor.docker.api.entities

import java.security.DigestInputStream

import org.parboiled2._
case class PortProtocol(port: Int, protocol: Option[String])
/**
 * Created by tldeti on 14-12-15.
 */
class DockerParser (val input:ParserInput) extends Parser{

    def Number = rule {
      capture(Digits) ~> (_.toInt)
    }

    def Digits = rule {
      oneOrMore(CharPredicate.Digit)
    }

    def PortWithProtocel = rule { Number ~ optional('/' ~ capture("tcp" | "udp")) ~>
      ((num: Int, str: Option[String]) => PortProtocol(num, str)) ~ EOI }


  // TODO write a file path rule
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
          ) ~ EOI
    }

    def DockerName = rule {
      oneOrMore(CharPredicate.AlphaNum | anyOf("""_-."""))
    }

    def NoIndexRepoTagLoc = rule {
      capture(oneOrMore(CharPredicate.AlphaNum | anyOf(""":_-."""))) ~ "/" ~ capture(DockerName) ~ optional(":" ~ capture(DockerName)) ~>
        ((path:String, repo:String, tag:Option[String]) =>
          RepoTagLocation.noIndexRepoTDefault(path ,repo, tag))
    }

    def IndexRepoLoc = rule {
      optional(capture(oneOrMore(CharPredicate.AlphaNum | anyOf("""_-."""))) ~ "/") ~ capture(DockerName) ~ optional(":" ~ capture(DockerName)) ~>
        ((namespace:Option[String], repo:String, tag:Option[String]) =>
          RepoTagLocation.indexRepoTDefault(namespace,repo,tag))
    }

  }