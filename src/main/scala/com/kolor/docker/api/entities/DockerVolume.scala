package com.kolor.docker.api.entities

import com.kolor.docker.api._
import play.api.libs.json._

trait DockerVolume extends DockerEntity {
  def containerPath: String
  def hostPath: String
}


final case class ContainerVolume(path:String)

final case class VolumeBind(containerPath:String,hostPath:Option[String],rw:Option[RoERw])

//sealed case class ContainerVolume(containerPath: String, hostPath: String = "") extends DockerVolume {
//  override def toString = s"$containerPath"
//}

sealed case class BindMountVolume(containerPath: String, hostPath: String, `type`: String = "ro") extends DockerVolume{
  override def toString = s"$containerPath:$hostPath:${`type`}"
}
		
object DockerVolume {

	  private val pattern = """^([^:]+):(/[^:]+):(ro|rw)$""".r




//	  def unapply(v: DockerVolume): Option[(String, String)] = {
//	    v match {
//	      case bind: BindMountVolume => Some((bind.containerPath, bind.hostPath))
//	      case vol: ContainerVolume => Some((vol.containerPath, ""))
//	      case _ => None
//	    }
//	  }
//
//	  def fromString(s: String): Option[BindMountVolume] = s match {
//	    case pattern(containerPath, hostPath, rwType) => Some(BindMountVolume(containerPath, hostPath, rwType))
//	    case _ => None
//	  }

}