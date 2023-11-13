package services

import cats.effect.unsafe.implicits.global
import cats.effect._
import cats.{Applicative, Monad, MonadError}
import com.datastax.oss.driver.api.core.{CqlSession, PagingIterable}
import cats.implicits._
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.data.UdtValue

import scala.collection.mutable
import java.util.UUID
import domain.Advert.AdVariant
import domain.ABTest._
import domain.Brand._
import persistence.CassandraDb
import persistence.PracticeDb._

import scala.jdk.CollectionConverters._
import java.time.{Duration, LocalDateTime, ZoneOffset}
import scala.concurrent.duration.DurationInt
import scala.util.Random

object TestService {
  sealed trait RequestResult
  final case object DatabaseConnectionError extends RequestResult
  final case object AddTestQuerySuccess extends RequestResult
  final case object AddTestQueryError extends RequestResult
  final case object TestIdNotFoundError extends RequestResult
  final case object DeploymentSuccess extends RequestResult
  final case object DeploymentError extends RequestResult

  // Algebra
  trait TestRoutesAlgebra[F[_]] {
    def addTest(newTest: NewTest): F[RequestResult]
    def deployTest(testId: TestId): F[RequestResult]
    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]]
    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]]
    def getTestById(testId: TestId, cols: List[String]): F[Either[RequestResult, ResultSet]]
    def getTestsByBrand(brand: BrandName): F[Either[RequestResult, ResultSet]]
    def getTestPerformance(testId: TestId): F[Either[RequestResult, ResultSet]]
  }

  def createTest(newTest: NewTest): Test = {
    val testId: TestId = TestId(UUID.randomUUID())
    val variantSpend: BigDecimal = newTest.testSpend / newTest.adTextVariants.length
    val adVariants: Vector[AdVariant] = newTest.adTextVariants.map(adText => AdVariant(UUID.randomUUID(), adText, variantSpend, false))
    val testSubmissionDate: LocalDateTime = LocalDateTime.now()
    val testStartDate: LocalDateTime = emptyDateTime
    val testStatus: TestStatus = Pending
    val testUpdate: TestUpdate = NoUpdate
    Test(testId, newTest.brandName, newTest.testName, adVariants, newTest.testSpend, newTest.testDuration,
      testSubmissionDate, testStartDate, testStatus, testUpdate)
  }

  // Interpreter
  case class TestRoutesInterpreter[F[_]: Async]()(implicit mc: MonadCancel[F, Throwable]) extends TestRoutesAlgebra[F]{
    def dbConnection(database: CassandraDb): Resource[F, CqlSession] = {
      Resource.make(MonadError[F, Throwable].catchNonFatal(database.session())) {
        connection => Applicative[F].pure(println(s"Releasing database connection: '$connection''"))
      }
    }
    val database = new CassandraDb()
    private val connectionDb = dbConnection(database)

    def addTest(newTest: NewTest): F[RequestResult] = {
      val test = createTest(newTest)

      Async[F].blocking {
        connectionDb.use { connection =>
          executeAddTestQuery(connection, test)
            .map(_ => AddTestQuerySuccess: RequestResult)
        }.handleErrorWith { error =>
          println(error)
          Monad[F].pure(AddTestQueryError: RequestResult)
        }
      }.flatten
    }

    def executeAddTestQuery(connection: CqlSession, test: Test): F[ResultSet] = {
      val statement = connection.prepare(
        """
       INSERT INTO abtest.test(testid, brandname, testname, advariants, testduration, testspend,
       testsubmissiondate, teststartdate, teststatus, testupdate)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """)
      val udtType = connection.getMetadata.getKeyspace("abtest").flatMap(_.getUserDefinedType("advariant")).get
      val udtValue: UdtValue = udtType.newValue()

      val adVariantsList = test.adVariants.toList.map(adVariant => {
        udtValue.setUuid("variantid", adVariant.variantid)
        udtValue.setString("adtext", adVariant.adtext)
        udtValue
      })

      val boundStatement = statement.bind(
        test.testId.testId, test.brandName.brandName, test.testName.testName, adVariantsList.asJava, test.testDuration, test.testSpend.bigDecimal,
        test.testSubmissionDate.toInstant(ZoneOffset.UTC), null, test.testStatus.toString, test.testUpdate.toString
      )

      val queryResult = connection.execute(boundStatement)
      Monad[F].pure(queryResult)
    }

    def deployTest(testId: TestId): F[RequestResult] = {
      Async[F].blocking {
        connectionDb.use { connection =>
          createPerformanceTable(connection, testId)
          val dataStream = initiateDataStream(connection, testId)
          val initiateDataStreamResult = Concurrent[F].start(dataStream)
          initiateDataStreamResult.flatMap(_ => Monad[F].pure(DeploymentSuccess: RequestResult))
        }.handleErrorWith { error =>
          println(error)
          Monad[F].pure(DeploymentError: RequestResult)
        }
      }.flatten
    }

    def createPerformanceTable(connection: CqlSession, testId: TestId): F[ResultSet] = {
      val createTable = {
        // using direct interpolation below as bound statements don't support dynamic injection of table names
        // current unsecure approach only to test functionality locally and it'll eliminated with the frontend build
        s"""
          CREATE TABLE IF NOT EXISTS abtest.test_performance_${testId.testId.toString.replace("-", "")} (
             testid uuid,
             timestamp timestamp,
             variantid uuid,
             performance double,
             PRIMARY KEY (testid, timestamp)
          );
        """
      }
      val queryResult = connection.execute(createTable)
      Monad[F].pure(queryResult)
    }

    def extractAdVariants(adVariants: List[UdtValue]): List[(UUID, String)] = {
      adVariants.map { udtValue =>
        val variantId = udtValue.getUuid("variantid")
        val variantText = udtValue.getString("adtext")
        (variantId, variantText)
      }
    }

    def getRandomVariantId(adVariants: List[(UUID, String)]): UUID = {
      val randomIndex = Random.nextInt(adVariants.length)
      val (randomVariantId, _) = adVariants(randomIndex)
      randomVariantId
    }

    def generateCTR(): Double = {
      val minCTR = 0.001
      val maxCTR = 0.10
      minCTR + (maxCTR - minCTR) * Random.nextDouble()
    }

    def initiateDataStream(connection: CqlSession, testId: TestId): F[TestStatus] = {
      val testData = getTestById(testId, List("testduration", "advariants")).map {
        case Right(result) =>
          val row = result.all()
          val duration = row.get(0).getDouble("testduration")
          val adVariants = row.get(0).getList("advariants", classOf[UdtValue])
          (duration, extractAdVariants(adVariants.asScala.toList))
      }

      testData.flatMap { data =>
        val testStart = LocalDateTime.now
        val duration = data._1.toLong
        val durationHours = Duration.ofHours(duration)
        val testEnd = testStart.plus(durationHours)

        def getDataAndCheckTime(): F[TestStatus] = {
          val currentTime = LocalDateTime.now
          if (currentTime.isBefore(testEnd)) {
            for {
              _ <- Temporal[F].sleep(1.second)
              _ <- Monad[F].pure {
                val statement = connection.prepare(
                  s"""
                     INSERT INTO abtest.test_performance_${testId.testId.toString.replace("-", "")} (testid, timestamp, variantid, performance)
                     VALUES (?, ?, ?, ?);
                   """
                )
                val boundStatement = statement.bind(testId.testId, currentTime.toInstant(ZoneOffset.UTC), getRandomVariantId(data._2), generateCTR())
                connection.execute(boundStatement)
              }
              result <- getDataAndCheckTime()
            } yield result
          } else {
            changeTestStatus(Ended)
          }
        }

        changeTestStatus(Live) *> getDataAndCheckTime()
      }
    }

    def changeTestStatus(status: TestStatus): F[TestStatus] = {
      Monad[F].pure(status) // not actual logic
    }

    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def getTestById(testId: TestId, cols: List[String]): F[Either[RequestResult, ResultSet]] = {
      Async[F].blocking {
        connectionDb.use { connection =>
          executeGetTestByIdQuery(connection, testId, cols).map[Either[RequestResult, ResultSet]] { result =>
            if (result.getAvailableWithoutFetching >= 1) {
              Right(result)
            } else {
              Left(TestIdNotFoundError)
            }
          }
        }.handleErrorWith { error =>
          println(error)
          Monad[F].pure(Left(DatabaseConnectionError))
        }
      }.flatten
    }

    def executeGetTestByIdQuery(connection: CqlSession, testId: TestId, cols: List[String]): F[ResultSet] = {
      val statement = cols match {
        case List("all") => connection.prepare(s"SELECT * FROM abtest.test WHERE testid = ?")
        case _ =>
          val colsString = cols.mkString(", ")
          connection.prepare(s"SELECT $colsString FROM abtest.test WHERE testid = ?")
      }
      val boundStatement = statement.bind(testId.testId)
      val queryResult = connection.execute(boundStatement)
      Monad[F].pure(queryResult)
    }

    def getTestsByBrand(brandName: BrandName): F[Either[RequestResult, ResultSet]] = {
      connectionDb.use { connection =>
        executeGetTestsByBrandQuery(connection, brandName).map[Either[RequestResult, ResultSet]] { result =>
          if (result.getAvailableWithoutFetching >= 1) {
            Right(result)
          } else {
            Left(TestIdNotFoundError)
          }
        }
      }.handleErrorWith { error =>
        println(error)
        Monad[F].pure(Left(DatabaseConnectionError))
      }
    }

    def executeGetTestsByBrandQuery(connection: CqlSession, brandName: BrandName): F[ResultSet] = {
      val statement = connection.prepare("SELECT * FROM abtest.test WHERE brandname = ?")
      val boundStatement = statement.bind(brandName.brandName)
      val queryResult = connection.execute(boundStatement)
      Monad[F].pure(queryResult)
    }

    def getTestPerformance(testId: TestId): F[Either[RequestResult, ResultSet]] = {
      ???
    }
  }
}





