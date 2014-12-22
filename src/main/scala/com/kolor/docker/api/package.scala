package com.kolor.docker


package object api {

  import com.kolor.docker.api.entities._
  import com.kolor.docker.api.json.Formats._
  
  implicit val dockerJsonFormats = com.kolor.docker.api.json.Formats
  
}