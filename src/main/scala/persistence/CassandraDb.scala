package persistence

import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress

class CassandraDb() {
  def session() = CqlSession.builder()
      .withKeyspace("abtest")
      .addContactPoint(new InetSocketAddress("localhost", 9042))
      .withLocalDatacenter("datacenter1")
      .build()
}
