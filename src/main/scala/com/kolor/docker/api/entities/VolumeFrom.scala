package com.kolor.docker.api.entities

import com.kolor.docker.api.entities.VolumeFrom.RoERw

/**
 * Created by tldeti on 14-12-3.
 */
case class VolumeFrom(containerName:ContainerName,roErw:RoERw){
  override def toString =
    s"${containerName.toString}:${roErw.toString}"
}

object VolumeFrom{
  def apply(str:String):Option[VolumeFrom] = {
    val arr = str.split(":")
    if(arr.length == 2){
      val rOpt = RoERw(arr(1))
      rOpt.map(r=>
        VolumeFrom(ContainerName(arr(0)),r)
      )
    }else
      None
  }

  sealed trait RoERw
  case object Ro extends RoERw{
    override def toString = "ro"
  }
  case object Rw extends RoERw{
    override def toString = "rw"
  }
  object RoERw{
    def apply(s:String) = s match {
      case "ro" => Some(Ro)
      case "rw" => Some(Rw)
      case _ => None
    }

  }
}