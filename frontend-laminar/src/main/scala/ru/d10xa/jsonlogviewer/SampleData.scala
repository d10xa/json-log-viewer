package ru.d10xa.jsonlogviewer

object SampleData {

  val jsonLogSample: String =
    """
|{"@timestamp":"2023-09-18T19:10:10.123456Z","message":"first line","logger_name":"MakeLogs","thread_name":"main","level":"INFO", "custom_field": "custom"}
|{"@timestamp":"2023-09-18T19:11:20.132318Z","message":"test","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
|{"@timestamp":"2023-09-18T19:12:30.132319Z","message":"debug msg","logger_name":"MakeLogs","thread_name":"main","level":"DEBUG"}
|prefix before json {"@timestamp":"2023-09-18T19:11:42.132320Z","message":"warning msg","logger_name":"MakeLogs","thread_name":"main","level":"WARNING"}
|{"@timestamp":"2023-09-18T19:13:42.132321Z","message":"warn msg","logger_name":"MakeLogs","thread_name":"main","level":"WARN"}
|{"@timestamp":"2023-09-18T19:14:42.137207Z","message":"error message","logger_name":"MakeLogs","thread_name":"main","level":"ERROR","stack_trace":"java.lang.RuntimeException: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\tat ru.d10xa.jsonlogviewer.MakeLogs$.main(MakeLogs.scala:9)\n\tat ru.d10xa.jsonlogviewer.MakeLogs.main(MakeLogs.scala)\nCaused by: java.lang.IllegalArgumentException: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\nCaused by: java.lang.ArithmeticException: hello\n\t... 2 common frames omitted\n", "duration": "30 seconds", "margin": "20px"}
|{"@timestamp":"2023-09-18T19:15:42.137207Z","message":"Request payload: {\"user\":\"alice\",\"action\":\"login\",\"meta\":{\"ip\":\"10.0.0.1\",\"ua\":\"Mozilla/5.0\"}}","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
|{"@timestamp":"2023-09-18T19:16:42.137207Z","message":"last line","logger_name":"MakeLogs","thread_name":"main","level":"INFO"}
|""".stripMargin

  val logfmtSample: String =
    """
      |@timestamp=2023-09-18T19:10:10.123456Z thread_name=main logger_name=MakeLogs first line custom_field=custom
      |@timestamp=2023-09-18T19:10:10.123456Z second line {"level":"INFO"}
      |""".stripMargin

  val csvSample: String =
    """@timestamp,level,logger_name,thread_name,message
      |2023-09-18T19:10:10.123456Z,INFO,MakeLogs,main,"first line, with comma"
      |2023-09-18T19:11:20.132318Z,INFO,MakeLogs,main,test
      |2023-09-18T19:12:30.132319Z,DEBUG,MakeLogs,main,debug msg
      |2023-09-18T19:13:42.132321Z,WARN,MakeLogs,main,warn msg
      |2023-09-18T19:14:42.137207Z,ERROR,MakeLogs,main,"error message,error details"
      |2023-09-18T19:15:42.137207Z,INFO,MakeLogs,main,last line
      |""".stripMargin
}
