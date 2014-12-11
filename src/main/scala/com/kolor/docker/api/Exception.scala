package com.kolor.docker.api

import com.kolor.docker.api.entities.ContainerId


case class DockerResponseCode(code: Int, message: String) extends Exception(s"Docker error (Code $code): $message")

trait DockerException extends Exception {
  def message: String
  override def getMessage():String = message
  override def getLocalizedMessage():String = message
}


sealed trait PrivateRegistryException extends DockerException
sealed trait PrivateRegResponseException extends PrivateRegistryException{
  def statusCode: Int
}

case class PRInternalException(message: String) extends PrivateRegResponseException{
  val statusCode = 500
}

case class NoAuthException(message: String) extends PrivateRegResponseException{
  val statusCode = 401
}

case class NotFoundException(message: String) extends PrivateRegResponseException{
  val statusCode = 404
}

case class WTFException(message: String,statusCode:Int) extends PrivateRegResponseException{
}

case class PRRequestException(message:String,cause:Option[Throwable]) extends PrivateRegistryException



trait DockerApiException extends DockerException {
  def client: DockerClient
}


case class InvalidContainerIdFormatException(message: String, id: String, cause: Option[Throwable] = None) extends DockerException {
	cause.map(initCause(_))
    def this(message: String) = this(message, null)
}

case class InvalidImageIdFormatException(message: String, id: String, cause: Option[Throwable] = None) extends DockerException {
	cause.map(initCause(_))
    def this(message: String) = this(message, null)
}

case class InvalidRepositoryTagFormatException(message: String, tag: String, cause: Option[Throwable] = None) extends DockerException {
	cause.map(initCause(_))
    def this(message: String) = this(message, null)
}

case class DockerRequestException(message: String, client: DockerClient, cause: Option[Throwable] = None, request: Option[dispatch.Req]) extends DockerApiException {
	cause.map(initCause(_))
  override def toString =
    message + cause.map(_.getMessage).getOrElse("")
}

case class DockerResponseParseError(message: String, client: DockerClient, response: String, cause: Option[Throwable] = None) extends DockerApiException {
	cause.map(initCause(_))
}

case class DockerInternalServerErrorException(client: DockerClient, message: String="internal server error") extends DockerApiException

case class DockerBadParameterException(message: String, client: DockerClient, request: dispatch.Req, cause: Option[Throwable] = None) extends DockerApiException {
	cause.map(initCause(_))
}

case class DockerAuthorizeException(message:String)

case class ContainerNotRunningException(id: ContainerId, client: DockerClient) extends DockerApiException {
  def message = s"container $id is not running"
}

case class NoSuchImageException(image: String, client: DockerClient) extends DockerApiException {
  def message = s"image $image doesn't exist"
}

case class DockerConflictException(message: String, client: DockerClient) extends DockerApiException

case class NoSuchContainerException(id: ContainerId, client: DockerClient) extends DockerApiException {
  def message = s"container $id doesn't exist"
}