
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.storage.StorageLevel
import com.github.catalystcode.fortis.spark.streaming.rss.{RSSInputDStream, RSSLink}
import org.apache.spark.{SparkConf, SparkContext}
import com.rometools.rome.feed.synd.{SyndEntry, SyndFeed}
import com.rometools.rome.io.{SyndFeedInput, XmlReader}
import java.net.URL
import java.sql.Date
import java.text.SimpleDateFormat

import org.apache.spark.sql.Encoders
import org.apache.spark.sql.types.{StringType, StructField, StructType}

import scala.collection.immutable.Map

object main {

  def parseTrends(
                   url: String,
                   requestHeaders: Map[String, String],
                   connectTimeout: Int = 1000,
                   readTimeout: Int = 1000
                 ): Seq[String] = {
    val connection = new URL(url).openConnection()
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    import scala.collection.JavaConverters._
    val reader = new XmlReader(connection, requestHeaders.asJava)

    val feed = new SyndFeedInput().build(reader)

    feed.getEntries().asScala.map(entry => entry.getTitle())
  }

  def main(args: Array[String]) {
    val durationSeconds = 10
    val conf = new SparkConf().setAppName("RSS Spark Application").setIfMissing("spark.master", "local[*]")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(durationSeconds))
    sc.setLogLevel("ERROR")

    // Setup a stream context.
    val batchInterval = 10

    val requestHeaders_ : Map[String, String] = Map[String, String]("User-Agent" -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
    val urls: Seq[String] = Seq("http://rss.cnn.com/rss/edition.rss")
    val stream = new RSSInputDStream(
      feedURLs = urls,
      requestHeaders = requestHeaders_,
      ssc,
      StorageLevel.MEMORY_ONLY,
      pollingPeriodInSeconds = batchInterval
    )

    val trends = parseTrends("https://trends.google.com/trends/trendingsearches/daily/rss?geo=US", requestHeaders_)
    trends.foreach(println)

    val trends_ = sc.broadcast(trends)

    stream.foreachRDD(rdd => {
      val spark = SparkSession.builder().appName(sc.appName).getOrCreate()
      import spark.sqlContext.implicits._

      rdd.map(e => (e.title, e.links.map(x => x.href).mkString(","), new SimpleDateFormat("d-M-y").format(new Date(e.publishedDate))))
        .filter(row => trends_.value.exists(word => row._1.toLowerCase().contains(word.toLowerCase())))
        .toDF("title", "links", "publishedDate")
        .show(20, false)
    })

    // run forever
    ssc.start()
    ssc.awaitTermination()

  }

}