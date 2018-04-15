import sys.process._
import java.io._
import java.net.URL
import java.nio.ByteBuffer

import org.apache.commons.io.IOUtils
import boopickle.Default._
import ammonite.ops.{Path, pwd}
import com.typesafe.scalalogging.LazyLogging
import org.rauschig.jarchivelib.{ArchiveFormat, ArchiverFactory, CompressionType}

import scala.math.Ordered.orderingToOrdered

case class Version(verString: String) extends Ordered[Version] {
  val version: Iterable[Int] = verString.split('.').map(_.toInt)

  override def compare(that: Version): Int = this.version compare that.version
}

object VersionsUtility extends LazyLogging {

  val INDEX_LINK: String = "http://hackage.haskell.org/packages/index.tar.gz"
  val INDEX_SOURCE_GZ: Path = pwd / 'data / "index.tar.gz"
  val INDEX_SOURCE_DIR: Path = pwd / 'data / 'index / "index"

  val VERSIONS_FILE: Path = pwd / 'data / "versions.obj"

  def updateIndex(): Unit = {
    logger.info("update index")

    new URL(INDEX_LINK) #> INDEX_SOURCE_GZ.toIO !!

    val archive = INDEX_SOURCE_GZ.toIO
    val destination = INDEX_SOURCE_DIR.toIO

    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    archiver.extract(archive, destination)
  }

  def updateVersions(): Map[String, Version] = {
    logger.info("update Versions")

    val indexDir = VersionsUtility.INDEX_SOURCE_DIR.toIO
    val packageNames = indexDir.listFiles.filter(_.isDirectory)

    val lastVersions = packageNames.flatMap(packagePath =>
      packagePath.listFiles.filter(_.isDirectory).map(versionPath =>
        (packagePath.getName, Version(versionPath.getName))
      )
    ).groupBy(_._1).mapValues(_.map(_._2).max)

    saveLastVersions(lastVersions)

    lastVersions

  }


  def loadCurrentVersions(): Map[String, Version] = {
    try {
      val fis = new FileInputStream(VERSIONS_FILE.toIO)

      val result = Unpickle[Map[String, Version]]
        .fromBytes(ByteBuffer.wrap(IOUtils.toByteArray(fis)))
      fis.close()

      result
    }
    catch {
      case e: Exception =>
        println(e.getMessage)
        Map()
    }
  }

  def saveLastVersions(lastVersions: Map[String, Version]): Unit = {
    val buf = Pickle.intoBytes(lastVersions)

    val oos = new FileOutputStream(VERSIONS_FILE.toIO)
    oos.write(buf.array())
    oos.close()
  }
}
