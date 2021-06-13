package com.example

import com.example.QoERegistry.QoEResponse
import spray.json.RootJsonFormat

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._

  implicit val bufferJsonFormat: RootJsonFormat[Buffer] = jsonFormat2(Buffer)
  implicit val qoeParamsJsonFormat: RootJsonFormat[QoEParams] = jsonFormat4(QoEParams)
  implicit val qoeResponse: RootJsonFormat[QoEResponse] = jsonFormat1(QoEResponse)
}
//#json-formats
