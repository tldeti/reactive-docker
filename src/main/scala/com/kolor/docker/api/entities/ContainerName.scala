package com.kolor.docker.api.entities

/**
 * Created by tldeti on 14-12-3.
 */
case class ContainerName(name:String) extends DockerEntity{
  override  def toString() = name
}