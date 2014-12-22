package com.kolor.docker.api

sealed trait DockerAttachable { }

case object Stdin extends DockerAttachable
case object Stdout extends DockerAttachable
case object Stderr extends DockerAttachable
