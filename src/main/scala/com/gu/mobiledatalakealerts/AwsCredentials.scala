package com.gu.mobiledatalakealerts

import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider

object AwsCredentials {

  val athenaCredentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("ophan"),
    new EnvironmentVariableCredentialsProvider()
  )

  val notificationCredentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("mobile"),
    new EnvironmentVariableCredentialsProvider()
  )

}
