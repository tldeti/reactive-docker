package com.kolor.docker.api.entities

import com.kolor.docker.api.InvalidRepositoryTagFormatException

import scala.util.Try


// although public repository is also a RepoLocation. but I don't know very much to it.
class RepositoryTag private[RepositoryTag] (val repo: String, val tag: Option[String]) extends DockerEntity {
  override def toString = {
		s"$repo"
	}

	def pRRepository = repo.split("/").take(2).lastOption

  override def equals(o: Any) = o match {
    case d:RepositoryTag => d.repo.eq(repo) && d.tag.eq(tag)
    case _ => false
  }
}

object RepositoryTag {
    val publicRegistryPattern = """^([\w_\-0-9\./]+):?([\w_\-0-9\.:]*)$""".r
    val privateRegistryPattern = """^(.+)/([\w_\-0-9\./]+):?([\w_\-0-9\.:]*)$""".r
	  val patternNone = """^(<none>):?(<none>)*$""".r

	  def apply(s: String): RepositoryTag = s match {
	    case publicRegistryPattern(repo, tag: String) => new RepositoryTag(repo, Some(tag))
	    case publicRegistryPattern(repo, _) => new RepositoryTag(repo, None)
      case privateRegistryPattern(host, repo, tag: String) => new RepositoryTag(s"$host/$repo", Some(tag))
//      case privateRegistryPattern(host, repo, _) => new RepositoryTag(s"$host/$repo", None)
	    case patternNone(_, _) => new RepositoryTag("none", Some("none"))	// there might be images with no tags (e.g. zombie images)
		  case _ => throw InvalidRepositoryTagFormatException(s"$s is an invalid repository tag", s)
	  }
	  
	  def unapply(tag: RepositoryTag): Option[String] = {
	    Some(tag.toString)
	  }
	  
	  def create(repo: String, tag: Option[String] = None) = new RepositoryTag(repo, tag)
}



/**
 * althought docker is identifier by repo level now.but when I see issues,it say should down to tag level.
 * TODO Find relate issues
 * @param path repo path, real location ,public repo resolve by namespace.private repo give by hand  eg:url.
 * @param namespace public repo: the username . private repo: library(default)
 * @param repo  repo name
 * @param tag
 */
abstract class RepoLocation(val path:String,val namespace:String,val repo:String,val tag: String)

case class NoIndexRepositoryLocation(override val namespace:String,override val path:String,override val repo:String,
																		 override val tag:String) extends RepoLocation(namespace,path,repo,tag)

object RepoLocation{
	def noIndexRepoT(s:String):Try[NoIndexRepositoryLocation] =
		new DockerParser(s).NoIndexRepoLoc.run()
}