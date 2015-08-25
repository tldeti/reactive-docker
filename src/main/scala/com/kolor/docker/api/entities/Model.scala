package com.kolor.docker.api.entities

import org.joda.time.DateTime


//desgin decision, only model the document says.not says use string or js value

case class DockerErrorInfo(code: Option[Int] = None, message: Option[String] = None) extends DockerEntity {
    override def toString = s"DockerErrorInfo(code=${code}, message=${message.getOrElse("")})"
    def isEmpty = code.isEmpty && message.isEmpty
  }

final case class DockerProgressInfo(current: Int, total: Int, start: Option[DateTime] = None) extends DockerEntity

final case class DockerStatusMessage(
    id: Option[String] = None,
    stream: Option[String] = None,
    status: Option[String] = None,
    from: Option[String] = None,
    time: Option[DateTime] = None,
    progress: Option[DockerProgressInfo] = None,
    error: Option[DockerErrorInfo] = None) extends DockerEntity {

    override def toString = s"DockerStatusMessage(id=${id.getOrElse("none")}, progress=${progress.getOrElse("none")}, stream=${stream.getOrElse("")}, status=${status.getOrElse("")}, error=${error.map(_.toString).getOrElse("none")})"
    def isError = error.map(e => e.code.nonEmpty || e.message.nonEmpty).getOrElse(false)
  }

final case class Container(
    id: ContainerId,
    image: IndexRepoLocation,
    names: Option[Seq[String]],
    command: String,
    created: DateTime,
    status: String,
    ports: Seq[String],
    sizeRw: Option[Long],
    sizeRootFs: Option[Long]) extends DockerEntity

final case class ContainerState(
    running: Boolean = false,
    pid: Int = 0,
    exitCode: Int = 0,
    startedAt: Option[org.joda.time.DateTime] = None,
    finishedAt: Option[org.joda.time.DateTime] = None,
    ghost: Boolean = false) extends DockerEntity

final case class ContainerNetworkConfiguration(
    ipAddress: Option[String],
    ipPrefixLen: Option[Int],
    gateway: Option[String],
    bridge: Option[String],
    portMapping: Option[Seq[String]] = None,
    ports: Option[Seq[DockerPortBinding]] = None) extends DockerEntity

final case class DockerHostIpPort(hostIp:String,hostPort:String)


final case class DockerPortBinding(privatePort: Int,  protocol: Option[String] = None, hosts:Seq[DockerHostIpPort]) extends DockerEntity

  /*
   * not the same as DockerPortBinding,ExposedPorts is used by  container linking.
   * may be multiple ExposedPorts
   * "ExposedPorts": { "<port>/<tcp|udp>: {}" }
   */
final case class ExposedPorts(privatePort: Int, protocol: Option[String] = None){
    override def toString =
      s"$privatePort${protocol.fold("")("/"+_)}:{}"
  }

  sealed trait ContainerNetworkingMode extends DockerEntity { def name: String }

  object ContainerNetworkingMode {
    case object Default extends ContainerNetworkingMode { val name = "bridge" }
    case object Bridge extends ContainerNetworkingMode { val name = "bridge" }
    case object Host extends ContainerNetworkingMode { val name = "host" }
    case object None extends ContainerNetworkingMode { val name = "none" }
  final case class Container(name: String) extends ContainerNetworkingMode {
      def container = "" // TODO: extract container from string: container:<containerIdOrName>
    }
  }

final case class ContainerRestartPolicy(
    name: String,
    maximumRetryCount: Int) extends DockerEntity


  // no Devices field
final case class ContainerHostConfiguration(
    privileged: Boolean = false,
    publishAllPorts: Boolean = false,
    binds: Option[Seq[VolumeBind]] = None,
    containerIdFile: Option[String] = None,
    lxcConf: Option[Map[String, String]] = None,
    networkMode: ContainerNetworkingMode = ContainerNetworkingMode.Default,
    volumesFrom: Option[Seq[VolumeFrom]] = None,
    restartPolicy: Option[ContainerRestartPolicy] = None,
    portBindings: Option[Seq[DockerPortBinding]] = None,
    links: Option[Seq[String]] = None,
    capAdd: Option[Seq[String]] = None, // new with 1.14
    capDrop: Option[Seq[String]] = None, // new with 1.14
    dns: Option[Seq[String]] = None,
    dnsSearch: Option[Seq[String]] = None
    ) extends DockerEntity

final case class ContainerInfo(
    id: ContainerId,
    image: String,
    config: ContainerConfiguration,
    state: ContainerState,
    networkSettings: ContainerNetworkConfiguration,
    hostConfig: ContainerHostConfiguration,
    created: DateTime,
    name: Option[String] = None,
    path: Option[String] = None,
    args: Option[Seq[String]] = None,
    resolveConfPath: Option[String] = None,
    hostnamePath: Option[String] = None,
    hostsPath: Option[String] = None,
    driver: Option[String] = None,
    volumes: Option[Seq[ContainerVolume]] = None,
    volumesRW: Option[Map[String, Boolean]] = None) extends DockerEntity

final case class ContainerChangelogRecord(
    path: String,
    kind: Int) extends DockerEntity

final case class DockerRawStreamChunk(channel: Int, size: Int, data: Array[Byte]) extends DockerEntity {
    def text = (new String(data, "utf-8")).trim
    override def toString = s"RawStreamChunk [$channel] '$text'"
  }

final case class DockerImage(
    id: String,
    parentId: Option[String],
    repoTags: Option[Seq[IndexRepoLocation]],
    created: DateTime,
    size: Long,
    virtualSize: Long) extends DockerEntity

final case class DockerImageInfo(
    id: ImageId,
    parent: Option[ImageId] = None,
    created: DateTime,
    container: Option[ContainerId],
    containerConfig: Option[ContainerConfiguration],
    dockerVersion: Option[String],
    author: Option[String],
    config: ContainerConfiguration,
    architecture: Option[String],
    size: Option[Long],
    comment: String) extends DockerEntity

final case class DockerImageHistoryInfo(
    id: ImageId,
    created: DateTime,
    createdBy: String,
    tags: Option[Seq[String]],
    size: Option[Long])

final case class DockerImageSearchResult(
    name: String,
    description: Option[String],
    isOfficial: Boolean = false,
    isTrusted: Boolean = false,
    starCount: Int = 0)

final case class DockerInfo(
    containers: Int,
    debug: Boolean,
    driver: String,
    driverStatus: Map[String, String],
    executionDriver: String,
    ipv4Forwarding: Boolean,
    images: Int,
    indexServerAddress: String,
    initPath: String,
    initSha1: String,
    kernelVersion: String,
    memoryLimit: Boolean,
    nEventsListener: Int,
    nFd: Int,
    nGoroutines: Int,
    swapLimit: Boolean,
    sockets: Seq[String] = Seq.empty // new with 1.13 - TODO: check exact format
    )

final case class DockerVersion(version: String, gitCommit: Option[String], goVersion: Option[String], arch: Option[String], kernelVersion: Option[String], os: Option[String], apiVersion: Option[String])
