package com.kolor.docker.api.json

import com.kolor.docker.api.entities._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

// docker remote api is not very strict.sometimes a jsarray location only have a element T,it
// become a T,not Seq[T]. sometime when no element, it become a null ,not no this field.

class ISODateTimeString(string: String) {
  def isoDateTime: DateTime = try {
    org.joda.time.format.ISODateTimeFormat.dateTime().parseDateTime(string)
  } catch {
    case e: IllegalArgumentException => org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis().parseDateTime(string)
    case t: Throwable => new org.joda.time.DateTime(0)
  }
}

trait PartialFormat[T ] extends Format[T] {
  def partialReads: PartialFunction[JsValue, JsResult[T]]

  def partialWrites: T => JsValue

  def writes(t: T): JsValue = partialWrites(t)

  def reads(json: JsValue) = partialReads.lift(json).getOrElse(JsError("unhandled json value"))
}


object Formats {
//  implicit def String2ISODateTime(s: String): ISODateTimeString = new ISODateTimeString(s)

  def int2Boolean(i: Int): Boolean = i match {
    case 0 => false
    case _ => true
  }

  def isoDateTimeFrom(str:String):Try[DateTime] =
    Try(DateTime.parse(str))

  def tryToJsResult[T](ty:Try[T]) =
    ty match {
      case Success(t) => JsSuccess(t)
      case Failure(x) => JsError(x.getMessage)
    }

  def toJsResultWithMsg[T](ty:Try[T],message:String) =
    ty match {
      case Success(t) => JsSuccess(t)
      case Failure(x) => JsError(message + ";" +x.getMessage)
    }

  implicit object ContainerIdFormat extends PartialFormat[ContainerId] {
    def partialReads: PartialFunction[JsValue, JsResult[ContainerId]] = {
      case o: JsString if o.value.nonEmpty => JsSuccess(ContainerId(o.as[String]))
      case _ => JsError("ContainerId is empty or invalid")
    }

    val partialWrites: PartialFunction[DockerEntity, JsValue] = {
      case oid: ContainerId => JsString(oid.id)
    }
  }

  implicit object ContainerNetworkingModeFormat extends PartialFormat[ContainerNetworkingMode] {
    def partialReads: PartialFunction[JsValue, JsResult[ContainerNetworkingMode]] = {
      case o: JsString => o.value.toLowerCase match {
        case "bridge" => JsSuccess(ContainerNetworkingMode.Bridge)
        case "host" => JsSuccess(ContainerNetworkingMode.Host)
        case "none" => JsSuccess(ContainerNetworkingMode.None)
        case s if s.startsWith("container:") => JsSuccess(ContainerNetworkingMode.Container(s))
        case _ => JsSuccess(ContainerNetworkingMode.Default)
      }
      case _ => JsError("ContainerNetworkingMode is empty or invalid")
    }

    val partialWrites: PartialFunction[DockerEntity, JsValue] = {
      case mode: ContainerNetworkingMode => JsString(mode.name)
    }
  }

  object IsoDateTimeStringFormat extends PartialFormat[DateTime]{
    def partialReads = {
      case JsString(s) => toJsResultWithMsg(isoDateTimeFrom(s), "not valid iso str")
    }

    override def partialWrites = (t:DateTime) =>
      JsString(t.toString)
  }

  implicit object ImageIdFormat extends PartialFormat[ImageId] {
    def partialReads: PartialFunction[JsValue, JsResult[ImageId]] = {
      case o: JsString if o.value.nonEmpty => JsSuccess(ImageId(o.as[String]))
      case _ => JsError("ImageId is empty or invalid")
    }

    val partialWrites: PartialFunction[DockerEntity, JsValue] = {
      case oid: ImageId => JsString(oid.id)
    }
  }

  implicit object IndexRepoLocationFormat extends PartialFormat[IndexRepoLocation] {
    def partialReads: PartialFunction[JsValue, JsResult[IndexRepoLocation]] = {
      case o: JsString if o.value.nonEmpty =>
        RepoTagLocation.indexRepoTParse(o.value) match {
          case scala.util.Success(repTLoc) =>JsSuccess(repTLoc.repoLocation)
          case scala.util.Failure(x) => JsError(s"index repoTParse error,${o.value}")
        }
      case _ => JsError("repository tag is empty or invalid")
    }

    val partialWrites: PartialFunction[DockerEntity, JsValue] = {
      case tag: IndexRepoLocation => JsString(tag.noTagImage)
    }
  }

  implicit val dockerHostIpPortFormat = Json.format[DockerHostIpPort]

//  implicit val dockerVolumnReader = Reads[DockerVolume] {
//    case JsString(s) => DockerVolume.fromString(s).map(JsSuccess(_)).getOrElse(JsError(ValidationError(s"not a valid Docker volumnStr")))
//    case x: JsObject => Json.fromJson[DockerVolume](x)(dockerVolumeReads)
//    case _ => JsError(ValidationError("error.expected.jsstringorjsobject"))
//  }

  // dockerVolume may have only a hostPath str,need fetch info from Map key.
//  val dockerVolumeMapReads = Reads[Map[String, DockerVolume]] {
//    case JsObject(fields) =>
//      fields.foldLeft[JsResult[Map[String, DockerVolume]]](JsSuccess(Map(): Map[String, DockerVolume])) { (dsJ, field) =>
//        field._2 match {
//          case JsNull => dsJ
//          case _ =>
//            for {
//              ds <- dsJ
//              d <- Json.fromJson[DockerVolume](field._2).orElse {
//                Json.fromJson[String](field._2).filterNot(_.contains(':')).flatMap(hostPath =>
//                  JsSuccess(ContainerVolume(field._1, hostPath))
//                )
//              }
//            } yield ds + ((field._1, d))
//        }
//      }
//    case _ => JsError(ValidationError("error.expected.jsobject"))
//  }

  import com.kolor.docker.api.codec.JsResultInstance._

import scalaz._
  import scalaz.std.list._

  // portBinding list is a Json fieds list,not json array
  implicit val dockerPortBindingFromFieldFormats = Format[Seq[DockerPortBinding]](
    Reads[Seq[DockerPortBinding]] {
      case JsObject(fields) =>
        Applicative[JsResult].traverse(fields.to[List]){ fie =>
          val portProtocel: Try[PortProtocol] = new DockerParser(fie._1).PortWithProtocel.run()
          portProtocel.map(pP =>
            fie._2 match {
              case JsNull => JsSuccess(DockerPortBinding(pP.port,pP.protocol,Nil))
              case JsArray(xs) =>
                Applicative[JsResult].traverse(xs.toList)(x =>
                  x.validate[DockerHostIpPort]
                ).map(hps =>
                  DockerPortBinding(pP.port, pP.protocol, hps)
                  )
              case _ => JsError("not valid docker port binding ") // TODO check logic
            }
          ).getOrElse(JsError("not valid PortProtocol"))
        }
      case _ =>
        JsError("not valid docker port binding")
    },
  // TODO check this getOrElse("tcp") is necessary
    Writes[Seq[DockerPortBinding]](pbs =>
      JsObject(
        pbs.map(pb=>
          s"${pb.privatePort}/${pb.protocol.getOrElse("tcp")}" -> Json.toJson(pb.hosts)
        )
      )
    )
  )

  implicit val exposedPortSeqFormats = Format[Seq[ExposedPorts]](
    Reads[Seq[ExposedPorts]] {
      case JsObject(fields) =>
        Applicative[JsResult].traverse(fields.toList)(fie =>
          new DockerParser(fie._1).PortWithProtocel.run() match {
            case scala.util.Success(x) =>
              fie._2 match {
                case JsObject(o) if o.size == 0 =>
                  JsSuccess(ExposedPorts(x.port, x.protocol))
                case _ =>
                  JsError(s"${fie._1} should have a empty object value")
              }
            case scala.util.Failure(x) => JsError(s"${fie._1} not valid,${x.getMessage}")
          }
        )
      case _ =>
        JsError("not valid exposedPort seq object")
    }, Writes[Seq[ExposedPorts]]((eps: Seq[ExposedPorts]) =>
      JsObject(
        eps.map(ep =>
          s"${ep.privatePort}${ep.protocol.fold("")("/" + _)}" -> Json.obj()
        )
      )
    )
  )

  val dateTimeToIsoWrite: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(dt: org.joda.time.DateTime): JsValue = JsString(org.joda.time.format.ISODateTimeFormat.dateTime().print(dt))
  }

  val dateTimeToMillis: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(dt: org.joda.time.DateTime): JsValue = JsNumber(dt.getMillis)
  }

  implicit val containerVolume:Format[Seq[ContainerVolume]] = Format[Seq[ContainerVolume]](
    Reads[Seq[ContainerVolume]]{
      case JsObject(fields) =>
        Applicative[JsResult].traverse(fields.to[List])(fie =>
          JsSuccess(ContainerVolume(fie._1))
        )
      case x =>
        JsError(s"not valid containerVolume ${Json.stringify(x)}")
    },
    Writes[Seq[ContainerVolume]]{
      cv => JsObject(
        cv.map(c =>
          c.path -> Json.obj()
        )
      )
    }
  )

  implicit val volumeBinds:Format[VolumeBind] = Format[VolumeBind](
    Reads[VolumeBind]{
      case JsString(str) =>
        val vbt:Try[VolumeBind] = new DockerParser(str).Binds.run()
        vbt match {
          case scala.util.Success(vb) => JsSuccess(vb)
          case scala.util.Failure(x) => JsError("volumeBind parse Error,"+x.getMessage)
        }
      case x =>
        JsError(s"VolumBind should be a string, ${Json.stringify(x)}")
    },
    Writes[VolumeBind]( vb =>
      JsString(s"${vb.hostPath}${vb.containerPath.fold("")(v=>s":$v")}${vb.rw.fold("")(v=>s":${v.toString}")}")
    )
  )

  //  val hostConfigPortBindingWrite: Writes[Map[String, DockerPortBinding]] = new Writes[Map[String, DockerPortBinding]] {
  //    def writes(ports: Map[String, DockerPortBinding]): JsValue = {
  //      val ret = Json.obj()
  //      ports.map {
  //        case (_, cfg) => Map(s"${cfg.privatePort}/${cfg.protocol.getOrElse("tcp")}" -> Json.arr(Json.obj("HostPort" -> cfg.publicPort, "HostIp" -> cfg.hostIp)))
  //      }
  //
  //      ret
  //    }
  //  }
  //
  //  val networkConfigPortBindingWrite: Writes[Seq[DockerPortBinding]] = new Writes[Seq[DockerPortBinding]] {
  //    def writes(ports: Seq[DockerPortBinding]): JsValue = {
  //      val ret = Json.obj()
  //      ports.map {
  //        case cfg => Map(s"${cfg.privatePort}/${cfg.protocol.getOrElse("tcp")}" -> Json.obj("HostPort" -> cfg.publicPort, "HostIp" -> cfg.hostIp))
  //      }
  //      ret
  //    }
  //  }

//  val containerConfigPortBindingWrite: Writes[Map[String, DockerPortBinding]] = new Writes[Map[String, DockerPortBinding]] {
//    def writes(ports: Map[String, DockerPortBinding]): JsValue = {
//      val ret = Json.obj()
//      ports.map {
//        case (_, cfg) => Map(s"${cfg.privatePort}/${cfg.protocol.getOrElse("tcp")}" -> Json.obj("HostPort" -> cfg.publicPort, "HostIp" -> cfg.hostIp))
//      }
//      ret
//    }
//  }

  implicit val dockerErrorInfoFmt = Format(
    (
      (__ \ "code").readNullable[Int] and
        (__ \ "message").readNullable[String])(DockerErrorInfo.apply _),
    Json.writes[DockerErrorInfo])

  implicit val dockerProgressInfoFmt = Format(
    (
      (__ \ "current").read[Int] and
        (__ \ "total").read[Int] and
        (__ \ "start").readNullable[Long].map(_.map(new org.joda.time.DateTime(_))))(DockerProgressInfo.apply _),
    Json.writes[DockerProgressInfo])


  implicit val dockerStatusMessageFmt = Format(
    (
      (__ \ "id").readNullable[String] and
        (__ \ "stream").readNullable[String] and
        (__ \ "status").readNullable[String] and
        (__ \ "from").readNullable[String] and
        (__ \ "time").readNullable[Long].map(_.map(new org.joda.time.DateTime(_))) and
        ((__ \ "progressDetail").readNullable[DockerProgressInfo] orElse Reads.pure(None)) and // we need this dirty hack here, as progressDetail sometimes is an empty object {} which is not handleded properly by readNullable
        ((__ \ "errorDetail").read[DockerErrorInfo].map(e => Some(e)) or (__ \ "error").readNullable[String].map(_.map(err => DockerErrorInfo(message = Some(err)))))
      )(DockerStatusMessage.apply _),
    Json.writes[DockerStatusMessage])

  implicit val dockerImageSearchResultFmt = Format(
    (
      (__ \ "name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "is_official").read[Boolean] and
        (__ \ "is_trusted").read[Boolean] and
        (__ \ "star_count").read[Int])(DockerImageSearchResult.apply _),
    Json.writes[DockerImageSearchResult])

  implicit val dockerImageHistoryInfoFmt = Format(
    (
      (__ \ "Id").read[ImageId](ImageIdFormat) and
        (__ \ "Created").read[Long].map(new org.joda.time.DateTime(_)) and
        (__ \ "CreatedBy").read[String] and
        (__ \ "tags").readNullable[Seq[String]] and
        (__ \ "Size").readNullable[Long])(DockerImageHistoryInfo.apply _),
    Json.writes[DockerImageHistoryInfo])


  implicit val containerChangelogFmt = Format(
    (
      (__ \ "Path").read[String] and
        (__ \ "Kind").read[Int])(ContainerChangelogRecord.apply _),
    Json.writes[ContainerChangelogRecord])

  implicit val containerRestartPolicyFmt = Format(
    (
      (__ \ "Name").read[String] and
        (__ \ "MaximumRetryCount").read[Int])(ContainerRestartPolicy.apply _),
    Json.writes[ContainerRestartPolicy])

  implicit val dockerVersionFmt = Format(
    (
      (__ \ "Version").read[String] and
        (__ \ "GitCommit").readNullable[String] and
        (__ \ "GoVersio ").readNullable[String] and
        (__ \ "Arch").readNullable[String] and
        (__ \ "KernelVersion").readNullable[String] and
        (__ \ "Os").readNullable[String] and
        (__ \ "ApiVersion").readNullable[String])(DockerVersion.apply _),
    Json.writes[DockerVersion])

  implicit val containerStateFmt = Format(
    (
      (__ \ "Running").read[Boolean] and
        (__ \ "Pid").read[Int] and
        (__ \ "ExitCode").read[Int] and
        (__ \ "StartedAt").readNullable[DateTime](IsoDateTimeStringFormat) and
        (__ \ "FinishedAt").readNullable[DateTime](IsoDateTimeStringFormat) and
        (__ \ "Ghost").read[Boolean].orElse(Reads.pure(false)))(ContainerState.apply _),
    (
      (__ \ "Running").write[Boolean] and
        (__ \ "Pid").write[Int] and
        (__ \ "ExitCode").write[Int] and
        (__ \ "StartedAt").writeNullable[org.joda.time.DateTime](dateTimeToIsoWrite) and
        (__ \ "FinishedAt").writeNullable[org.joda.time.DateTime](dateTimeToIsoWrite) and
        (__ \ "Ghost").write[Boolean])(unlift(ContainerState.unapply)))

  implicit val containerNetworkConfigFmt = Format(
    (
      ((__ \ "IPAddress").readNullable[String] or (__ \ "IPAddress").readNullable[String]) and
        ((__ \ "IPPrefixLen").readNullable[Int] or (__ \ "IPPrefixLen").readNullable[Int]) and
        (__ \ "Gateway").readNullable[String] and
        (__ \ "Bridge").readNullable[String] and
        (__ \ "PortMapping").readNullable[Seq[String]] and
        (__ \ "Ports").readNullable[Seq[DockerPortBinding]])(ContainerNetworkConfiguration.apply _),
    (
      (__ \ "IPAddress").writeNullable[String] and
        (__ \ "IPPrefixLen").writeNullable[Int] and
        (__ \ "Gateway").writeNullable[String] and
        (__ \ "Bridge").writeNullable[String] and
        (__ \ "PortMapping").writeNullable[Seq[String]] and
        (__ \ "Ports").writeNullable[Seq[DockerPortBinding]])(unlift(ContainerNetworkConfiguration.unapply)))

  implicit val dockerAuthFmt = Format(
    (
      (__ \ "Username").read[String] and
        (__ \ "Password").read[String] and
        (__ \ "Email").read[String] and
        (__ \ "ServerAddress").read[String])(DockerAuth.apply _),
    (
      (__ \ "Username").write[String] and
        (__ \ "Password").write[String] and
        (__ \ "Email").write[String] and
        (__ \ "ServerAddress").write[String])(unlift(DockerAuth.unapply)))

  implicit val containerNameFmt = Json.format[ContainerName]

  implicit object volumnFromWrites extends Writes[VolumeFrom] {
    def writes(d: VolumeFrom): JsValue = JsString(d.toString)
  }

  implicit val volumnFromReader = Reads[VolumeFrom](js => js match {
    case JsString(s) =>
      VolumeFrom(s).map(JsSuccess(_)).getOrElse(JsError(ValidationError(s"not a valid volumnFrom str: $s")))
    case _ => JsError(ValidationError("error.expected.jsstring"))
  })


  implicit val containerHostConfigFmt = Format(
    (
      (__ \ "Privileged").read[Boolean] and
        (__ \ "PublishAllPorts").read[Boolean] and
        (__ \ "Binds").readNullable[Seq[VolumeBind]] and
        (__ \ "ContainerIdFile").readNullable[String] and
        (__ \ "LxcConf").readNullable[Map[String, String]] and
        (__ \ "NetworkMode").read[ContainerNetworkingMode](ContainerNetworkingModeFormat).orElse(Reads.pure(ContainerNetworkingMode.Default)) and
        (__ \ "VolumesFrom").readNullable[Seq[VolumeFrom]].orElse(Reads.pure(None)) and
        (__ \ "RestartPolicy").readNullable[ContainerRestartPolicy](containerRestartPolicyFmt) and
        (__ \ "PortBindings").readNullable[Seq[DockerPortBinding]] and
        (__ \ "Links").readNullable[Seq[String]] and
        (__ \ "CapAdd").readNullable[Seq[String]] and
        (__ \ "CapDrop").readNullable[Seq[String]] and
        (__ \ "Dns").readNullable[Seq[String]] and
        (__ \ "DnsSearch").readNullable[Seq[String]])(ContainerHostConfiguration.apply _),
    (
      (__ \ "Privileged").write[Boolean] and
        (__ \ "PublishAllPorts").write[Boolean] and
        (__ \ "Binds").writeNullable[Seq[VolumeBind]] and
        (__ \ "ContainerIdFile").writeNullable[String] and
        (__ \ "LxcConf").writeNullable[Map[String, String]] and
        (__ \ "NetworkMode").write[ContainerNetworkingMode](ContainerNetworkingModeFormat) and
        (__ \ "VolumesFrom").writeNullable[Seq[VolumeFrom]] and
        (__ \ "RestartPolicy").writeNullable[ContainerRestartPolicy](containerRestartPolicyFmt) and
        (__ \ "PortBindings").writeNullable[Seq[DockerPortBinding]] and
        (__ \ "Links").writeNullable[Seq[String]] and
        (__ \ "CapAdd").writeNullable[Seq[String]] and
        (__ \ "CapDrop").writeNullable[Seq[String]] and
        (__ \ "Dns").writeNullable[Seq[String]] and
        (__ \ "DnsSearch").writeNullable[Seq[String]])(unlift(ContainerHostConfiguration.unapply)))

  implicit val containerConfigFmt = Format(
    (
      (__ \ "Image").readNullable[String] and
        (__ \ "Cmd").readNullable[Seq[String]] and
        (__ \ "Hostname").readNullable[String] and
        (__ \ "User").readNullable[String] and
        (__ \ "Memory").readNullable[Long] and
        (__ \ "MemorySwap").readNullable[Long] and
        (__ \ "AttachStdin").readNullable[Boolean] and
        (__ \ "AttachStdout").readNullable[Boolean] and
        (__ \ "AttachStderr").readNullable[Boolean] and
        //(__ \ "PortSpecs").readNullable[Seq[String]] and
        (__ \ "Tty").readNullable[Boolean] and
        (__ \ "OpenStdin").readNullable[Boolean] and
        (__ \ "StdinOnce").readNullable[Boolean] and
        (__ \ "HostConfig").readNullable[ContainerHostConfiguration] and
        (__ \ "Env").readNullable[Seq[String]] and
        (__ \ "Volumes").readNullable[Seq[ContainerVolume]] and
        (__ \ "WorkingDir").readNullable[String] and
        (__ \ "ExposedPorts").readNullable[Seq[ExposedPorts]](exposedPortSeqFormats) and
        (__ \ "Entrypoint").readNullable[Seq[String]] and
        (__ \ "NetworkDisabled").readNullable[Boolean] and
        (__ \ "OnBuild").readNullable[Seq[String]])(ContainerConfiguration.apply _),
    (
      (__ \ "Image").writeNullable[String] and
        (__ \ "Cmd").writeNullable[Seq[String]] and
        (__ \ "Hostname").writeNullable[String] and
        (__ \ "User").writeNullable[String] and
        (__ \ "Memory").writeNullable[Long] and
        (__ \ "MemorySwap").writeNullable[Long] and
        (__ \ "AttachStdin").writeNullable[Boolean] and
        (__ \ "AttachStdout").writeNullable[Boolean] and
        (__ \ "AttachStderr").writeNullable[Boolean] and
        //(__ \ "PortSpecs").writeNullable[Seq[String]] and
        (__ \ "Tty").writeNullable[Boolean] and
        (__ \ "OpenStdin").writeNullable[Boolean] and
        (__ \ "StdinOnce").writeNullable[Boolean] and
        (__ \ "HostConfig").writeNullable[ContainerHostConfiguration] and
        (__ \ "Env").writeNullable[Seq[String]] and
//        (__ \ "Dns").writeNullable[String] and
        (__ \ "Volumes").writeNullable[Seq[ContainerVolume]] and
        (__ \ "WorkingDir").writeNullable[String] and
        (__ \ "ExposedPorts").writeNullable[Seq[ExposedPorts]] and
        (__ \ "Entrypoint").writeNullable[Seq[String]] and
        (__ \ "NetworkDisabled").writeNullable[Boolean] and
        (__ \ "OnBuild").writeNullable[Seq[String]])(unlift(ContainerConfiguration.unapply)))

  implicit val containerInfoFmt = Format(
    (
      ((__ \ "ID").read[ContainerId] or (__ \ "id").read[ContainerId] or (__ \ "Id").read[ContainerId]) and
        (__ \ "Image").read[String] and
        (__ \ "Config").read[ContainerConfiguration] and
        (__ \ "State").read[ContainerState] and
        (__ \ "NetworkSettings").read[ContainerNetworkConfiguration] and
        (__ \ "HostConfig").read[ContainerHostConfiguration] and
        (__ \ "Created").read[DateTime](IsoDateTimeStringFormat) and
        (__ \ "Name").readNullable[String].map(o => o.map(_.stripPrefix("/"))) and
        (__ \ "Path").readNullable[String] and
        (__ \ "Args").readNullable[Seq[String]] and
        (__ \ "ResolvConfPath").readNullable[String] and
        (__ \ "HostnamePath").readNullable[String] and
        (__ \ "HostsPath").readNullable[String] and
        (__ \ "Driver").readNullable[String] and
        (__ \ "Volumes").readNullable[Seq[ContainerVolume]] and
        (__ \ "VolumesRW").readNullable[Map[String, Boolean]])(ContainerInfo.apply _),
    (
      (__ \ "ID").write[ContainerId] and
        (__ \ "Image").write[String] and
        (__ \ "Config").write[ContainerConfiguration] and
        (__ \ "State").write[ContainerState] and
        (__ \ "NetworkSettings").write[ContainerNetworkConfiguration] and
        (__ \ "HostConfig").write[ContainerHostConfiguration] and
        (__ \ "Created").write[DateTime](dateTimeToMillis) and
        (__ \ "Name").writeNullable[String] and
        (__ \ "Path").writeNullable[String] and
        (__ \ "Args").writeNullable[Seq[String]] and
        (__ \ "ResolvConfPath").writeNullable[String] and
        (__ \ "HostnamePath").writeNullable[String] and
        (__ \ "HostsPath").writeNullable[String] and
        (__ \ "Driver").writeNullable[String] and
        (__ \ "Volumes").writeNullable[Seq[ContainerVolume]] and
        (__ \ "VolumesRW").writeNullable[Map[String, Boolean]])(unlift(ContainerInfo.unapply)))

  implicit val containerFmt = Format(
    (
      (__ \ "Id").read[ContainerId] and
        (__ \ "Image").read[IndexRepoLocation] and
        (__ \ "Names").read[JsArray].map {
          case arr if (arr.value.size > 0) => Some(arr.value.seq.map {
            case JsString(s) => s.stripPrefix("/")
            case _ => ""
          }.filter(_.nonEmpty))
          case _ => None
        } and
        ((__ \ "Command").read[String] or (__ \ "Command").read[Boolean].map(_.toString)) and
        (__ \ "Created").read[Long].map(new org.joda.time.DateTime(_)) and
        (__ \ "Status").read[String] and
        (__ \ "Ports").read[JsArray].map { arr =>
          Seq.empty[String]
        } and
        (__ \ "SizeRw").readNullable[Long] and
        (__ \ "SizeRootFs").readNullable[Long])(Container.apply _),
    Json.writes[Container])

  implicit val imageFmt: Format[DockerImage] = Format(
    ((__ \ "Id").read[String] and
      (__ \ "ParentId").read[Option[String]] and
      (__ \ "RepoTags").readNullable[Seq[IndexRepoLocation]] and
      (__ \ "Created").read[Long].map(new org.joda.time.DateTime(_)) and
      (__ \ "Size").read[Long] and
      (__ \ "VirtualSize").read[Long])(DockerImage.apply _),
    Json.writes[DockerImage])

  implicit val imageInfoFmt: Format[DockerImageInfo] = Format(
    (
      (__ \ "id").read[ImageId](ImageIdFormat) and
        (__ \ "parent").readNullable[ImageId](ImageIdFormat) and
        (__ \ "created").read[DateTime](IsoDateTimeStringFormat) and
        (__ \ "container").readNullable[ContainerId](ContainerIdFormat) and
        (__ \ "container_config").readNullable[ContainerConfiguration] and
        (__ \ "docker_version").readNullable[String] and
        (__ \ "author").readNullable[String] and
        (__ \ "config").read[ContainerConfiguration] and
        (__ \ "architecture").readNullable[String] and
        (__ \ "Size").readNullable[Long] and
        (__ \ "Comment").read[String].orElse(Reads.pure("")))(DockerImageInfo.apply _),
    Json.writes[DockerImageInfo])

  implicit val dockerInfoFmt: Format[DockerInfo] = Format(
    ((JsPath \ "Containers").read[Int] and
      (JsPath \ "Debug").read[Int].map(int2Boolean(_)) and
      (JsPath \ "Driver").read[String] and
      (JsPath \ "DriverStatus").read[JsArray].map { arr =>
        val items = arr(0).asOpt[JsArray]
        val m = items.map(el => Map(el(0).asOpt[String].getOrElse("") -> el(1).asOpt[String].getOrElse("")))
        m.map(_.filterKeys(_.nonEmpty)).getOrElse(Map.empty)
      }.orElse(Reads.pure(Map.empty[String, String])) and
      (JsPath \ "ExecutionDriver").read[String] and
      (JsPath \ "IPv4Forwarding").read[Int].map(int2Boolean(_)) and
      (JsPath \ "Images").read[Int] and
      (JsPath \ "IndexServerAddress").read[String] and
      (JsPath \ "InitPath").read[String] and
      (JsPath \ "InitSha1").read[String] and
      (JsPath \ "KernelVersion").read[String] and
      (JsPath \ "MemoryLimit").read[Int].map(int2Boolean(_)) and
      (JsPath \ "NEventsListener").read[Int] and
      (JsPath \ "NFd").read[Int] and
      (JsPath \ "NGoroutines").read[Int] and
      (JsPath \ "SwapLimit").read[Int].map(int2Boolean(_)) and
      (JsPath \ "Sockets").read[Seq[String]].orElse(Reads.pure(Seq.empty[String])))(DockerInfo.apply _),
    Json.writes[DockerInfo])
}