package com.kolor.docker.api

import com.kolor.docker.api.entities._
import com.ning.http.client.Response
import dispatch.{Http, host, _}
import play.api.libs.json.Format

import scala.concurrent.ExecutionContext


final case class DockerRegistry(host: String, port: Int, version: String)

object DockerRegistry {
  def apply(host: String, port: Int): DockerRegistry =
    new DockerRegistry(host, port, "v1")
}

object PrivateRegistryEndpoint {
  def baseReq(implicit registry: DockerRegistry) =
    host(registry.host, registry.port) / registry.version secure

  def deleteRepoTag(r: RepTWithNS)(implicit registry: DockerRegistry) = {
    baseReq / "repositories" / r.namespace / r.repoTag.repo / "tags" / r.repoTag.tag.getOrElse("latest")
  }

  def deleteRepo(r: RepTWithNS)(implicit registry: DockerRegistry) = {
    baseReq / "repositories" / r.namespace / r.repoTag.repo / "tags"
  }
}

/**
 * Created by tldeti on 14-12-10.
 */
trait PrivateRegistryApi {
  self: DockerClient =>
  import com.kolor.docker.api.json.Formats._
  def pRRequest(req: Req)(implicit exec:ExecutionContext):Future[Either[PrivateRegistryException,Response]] =
    Http(req).map(resp =>
      resp match {
        case x if x.getStatusCode == 200 => Right(x)
        case x if x.getStatusCode == 500 => Left(PRInternalException(resp.getResponseBody))
        case x if x.getStatusCode == 401 => Left(NoAuthException(resp.getResponseBody))
        case x => Left(WTFException(resp.getResponseBody, resp.getStatusCode))
      }
    ).recover {
      case x: Throwable =>
        Left(PRRequestException(x.getMessage, Some(x)))
    }

  private def authHeaderMap(auth:DockerAuth) = auth match {
    case DockerAnonymousAuth => Map()
    case data => Map("X-Registry-Auth" -> data.asBase64Encoded)
  }


  def deleteRepoTag(r:RepTWithNS)(
    implicit d: DockerRegistry, auth: DockerAuth,exec:ExecutionContext
    ):Future[Either[PrivateRegistryException,Unit]] = {
    val req = PrivateRegistryEndpoint.deleteRepoTag(r).DELETE <:< authHeaderMap(auth)
    pRRequest(req).map(x =>
      x.right.map(_=>())
    )
  }

  /**
   * from https://github.com/docker/docker-registry/issues/45 , delete request is
   * DELETE /v1/repositories/<namespace>/<path:repository>/tags , not
   * DELETE /v1/repositories/<namespace>/<path:repository>/   which in specs
   */
  def deleteRepo(r:RepTWithNS)(
    implicit d: DockerRegistry, auth: DockerAuth,exec:ExecutionContext): Future[Either[PrivateRegistryException, Unit]] = {
    val req = PrivateRegistryEndpoint.deleteRepo(r).DELETE <:< authHeaderMap(auth)
    pRRequest(req).map { x=>
      x.right.map(_=>())
    }
  }


}
