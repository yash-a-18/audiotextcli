import zio.cli._
import zio.cli.HelpDoc.Span.text
import zio.Console.printLine
import cats.instances.list

// object of your app must extend ZIOCliDefault
object AudioTextCli extends ZIOCliDefault :

  sealed trait CommandType
  object CommandType :
    case class Record(listAvailableAudioDevices:Boolean) extends CommandType
    case class Whisper(wavfile: String) extends CommandType

  /**
   * First we define the commands of the Cli. To do that we need:
   *    - Create command options
   *    - Create command arguments
   *    - Create help (HelpDoc) 
   */
//   val options: Options[Boolean] = Options.boolean("local").alias("l")
  val waveFileArg: Args[String] = Args.text("wave_file")
  val help: HelpDoc = HelpDoc.p("Creates a copy of an existing repository")
  
  val record_cmd: Command[CommandType] = Command("record",Options.boolean("listDevices").alias("l"))
    .withHelp(help) //no options or arguments
    .map(listDevices => CommandType.Record(listDevices)) //map to CommandType.Record

  val whisper_cmd:Command[CommandType.Whisper] = Command("whisper",waveFileArg)
    .withHelp(help)
    .map(wavefile => CommandType.Whisper(wavefile)) //no options or arguments

  val audio_command: Command[CommandType] = Command("audio")
    .withHelp(help)
    .subcommands(record_cmd , whisper_cmd)
    .map{_.asInstanceOf[CommandType]} //map to CommandType

  
  // Define val cliApp using CliApp.make
  val cliApp = CliApp.make(
    name = "Sample Git",
    version = "1.1.0",
    summary = text("Sample implementation of git clone"),
    command = audio_command
  ) {
        // Implement logic of CliApp
      case r:CommandType.Record => if(r.listAvailableAudioDevices) {AudioRecorder.listAvailableAudioDevices();printLine("Available audio devices listed.")}
        else AudioRecorder.startRecording() ; printLine("Recording completed.")
      case w:CommandType.Whisper => 
        val result = WhisperTranscriber.transcribeAudioFile(w.wavfile)
        println(result match {
          case Right(transcription) => s"Transcription: $transcription"
          case Left(error) => s"Error: $error"
        })
        printLine(s"whispered  this file ${w.wavfile}")

    }