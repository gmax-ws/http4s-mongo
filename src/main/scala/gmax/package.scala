import com.typesafe.config.Config
import org.http4s.server.middleware.CORSConfig

import java.io.InputStream
import java.security.{KeyStore, Security}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.jdk.CollectionConverters._
import scala.util.Using

package object gmax {

  private def sslContext(ksStream: InputStream, keystorePassword: String, keyManagerPass: String): SSLContext = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, keystorePassword.toCharArray)

    val kmf = KeyManagerFactory.getInstance(
      Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
        .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

    kmf.init(ks, keyManagerPass.toCharArray)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)
    context
  }

  def ssl(cfg: Config): SSLContext =
    Using(this.getClass.getResourceAsStream("/" + cfg.getString("keystore"))) { ksStream =>
      sslContext(ksStream,
        cfg.getString("keystorePassword"),
        cfg.getString("keyManagerPass"))
    }.getOrElse(null)

  def cors(cfg: Config): CORSConfig = CORSConfig(
    anyOrigin = cfg.getBoolean("anyOrigin"),
    anyMethod = cfg.getBoolean("anyMethod"),
    allowedMethods = Option(cfg.getStringList("allowedMethods").asScala.toSet),
    allowCredentials = cfg.getBoolean("allowCredentials"),
    maxAge = cfg.getDuration("maxAge").getSeconds) //1.day.toSeconds)

  def uri(cfg: Config): String = {
    val username = cfg.getString("username")
    val password = cfg.getString("password")
    val authSource = cfg.getString("authSource")
    val host = cfg.getString("host")
    val port = cfg.getInt("port")
    val authMechanism = cfg.getString("authMechanism")
    s"mongodb://$username:$password@$host:$port/?authSource=$authSource&authMechanism=$authMechanism"
  }
}
