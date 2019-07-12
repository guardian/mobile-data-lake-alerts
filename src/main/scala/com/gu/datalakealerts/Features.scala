package com.gu.datalakealerts

import java.time.LocalDate

import com.amazonaws.services.athena.model.ResultSet
import com.gu.datalakealerts.Platforms.{ Android, Platform }

object Features {

  val yesterday: LocalDate = LocalDate.now().minusDays(1)
  val allFeaturesWithMonitoring: List[Feature] = List(FrictionScreen)

  def featureToMonitor(featureId: String): Feature = {
    allFeaturesWithMonitoring
      .find(feature => feature.id == featureId)
      .getOrElse(throw new RuntimeException(s"Invalid feature specified: features with monitoring are ${allFeaturesWithMonitoring.map(_.id)}"))
  }

  case class MonitoringQuery(query: String, minimumImpressionsThreshold: Int)
  case class MonitoringQueryResult(resultIsAcceptable: Boolean, additionalDebugInformation: Option[String])

  sealed trait Feature {
    val id: String
    def monitoringQuery(platform: Platform): MonitoringQuery
    def monitoringQueryResult(resultSet: ResultSet, minimumImpressionsThreshold: Int): MonitoringQueryResult
  }

  case object FrictionScreen extends Feature {

    val id = "friction_screen"

    def monitoringQuery(platform: Platform): MonitoringQuery = {

      val query = s"""
        |select browser_version, count (distinct page_view_id) as friction_screen_impressions
        |from clean.pageview
        |cross join unnest (component_events) x (c)
        |where received_date = date '$yesterday'
        |and device_type like '%${platform.id.toUpperCase}%'
        |and c.component.type like '%APP_SCREEN%'
        |and c.component.campaign_code like '%friction%' and c.component.campaign_code like '%subscription_screen%'
        |group by 1""".stripMargin

      val minimumImpressionsThreshold = platform match {
        case Android => 35000
        case iOS => 40000
      }

      MonitoringQuery(query, minimumImpressionsThreshold)

    }

    def monitoringQueryResult(resultSet: ResultSet, minimumImpressionsThreshold: Int): MonitoringQueryResult = {

      val impressionCountsByAppVersion = ImpressionCounts.getImpressionCounts(resultSet)
      val totalImpressions = impressionCountsByAppVersion.map(_.impressions).sum
      val resultIsAcceptable = totalImpressions > minimumImpressionsThreshold

      val additionalDebugInfo = if (!resultIsAcceptable) Some {
        s"""
           |Expected there to be at least $minimumImpressionsThreshold impressions, but only found $totalImpressions impressions.
         """.stripMargin
      }
      else {
        None
      }

      MonitoringQueryResult(resultIsAcceptable, additionalDebugInfo)

    }

  }

}