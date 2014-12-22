package com.kolor.docker.api.entities

import com.kolor.docker.api.InvalidRepositoryTagFormatException

import scala.util.Try


sealed abstract class RepoLocation(val pathOpt:Option[String],val namespaceOpt:Option[String],val repoName:String) extends DockerEntity

case class IndexRepoLocation(override val namespaceOpt:Option[String],override val repoName:String) extends RepoLocation(None,namespaceOpt,repoName){
	override def toString =
		s"${namespaceOpt.fold("")(n => s"$n/")}$repoName"
}
case class NoIndexRepoLocation(path:String, namespace:String,override val repoName:String
																) extends RepoLocation(Some(path),Some(namespace),repoName){
	override def toString =
		s"$pathOpt/$repoName"
}

/**
 * althought docker is identifier by repo level now.but when I see issues,it say should down to tag level.
 * TODO Find relate issues
 */
sealed abstract class RepoTagLocation(val repoLocation:RepoLocation ,val tag: String)

case class IndexRepoTagLocation(override val repoLocation: IndexRepoLocation,override val   tag: String) extends RepoTagLocation(repoLocation,tag){
	override def toString = s"$repoLocation:$tag"
}

case class NoIndexRepoTagLocation(override val  repoLocation: NoIndexRepoLocation,override val tag:String ) extends RepoTagLocation(repoLocation,tag){
	override def toString = s"$repoLocation:$tag"
}

//case class NoIndexRepositoryTagLocation(override val namespace:String,override val path:String,override val repo:String,
//																		 override val tag:String) extends RepoTagLocation(namespace,path,repo,tag)

object RepoTagLocation{
	def noIndexRepoTParse(s:String):Try[NoIndexRepoTagLocation] =
		new DockerParser(s).NoIndexRepoTagLoc.run()
	
	def indexRepoTParse(s:String):Try[IndexRepoTagLocation] =
		new DockerParser(s).IndexRepoLoc.run()

	def noIndexRepoTDefault(path:String,repo:String,tag:Option[String]) =
		NoIndexRepoTagLocation(NoIndexRepoLocation("library",path,repo),tag.getOrElse("lastest"))

	def indexRepoTDefault(namespace:Option[String],repo:String,tag:Option[String]) =
		IndexRepoTagLocation(IndexRepoLocation(namespace,repo),tag.getOrElse("latest"))

	def indexRepoT(repo:String) =
		indexRepoTDefault(None,repo,None)
}