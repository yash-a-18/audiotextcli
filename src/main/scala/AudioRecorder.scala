import javax.sound.sampled.*
import java.io.IOException
import java.util.Scanner
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration.*
import better.files.{File => BFile, *}
import scala.sys.process.*
import scala.util.{Try, Success, Failure}

object AudioRecorder :
  
  // Use Scala's default execution context
  private implicit val executionContext: ExecutionContext = ExecutionContext.global
  
  // Audio format configuration
  // private val sampleRate = 44100.0f
  private val sampleRate = 16000.0f //whisper works best at 16khz rather than 44.1khz
  private val sampleSizeInBits = 16
  private val channels = 1  // mono (whisper works best in mono)
  private val signed = true
  private val bigEndian = false
  
  private val audioFormat = new AudioFormat(
    sampleRate, 
    sampleSizeInBits, 
    channels, 
    signed, 
    bigEndian
  )
  
  private var targetDataLine: Option[TargetDataLine] = None
  private var audioInputStream: Option[AudioInputStream] = None
  private var recordingFuture: Option[Future[Unit]] = None
  private var isRecording = false
  
  def startRecording(): Either[String, Unit] = {
    val outputFile = generateTimestampedFilename("wav")
    startRecording(outputFile)
  }
  def startRecording(outputFile: String): Either[String, Unit] = {
    
    def setupAudioLine(): Try[TargetDataLine] = Try {
      println(s"Starting audio recording to: $outputFile")
      println(s"Audio format: ${audioFormat.getSampleRate()}Hz, ${audioFormat.getSampleSizeInBits()}-bit, ${audioFormat.getChannels()} channels")
      
      val dataLineInfo = new DataLine.Info(classOf[TargetDataLine], audioFormat)
      
      if (!AudioSystem.isLineSupported(dataLineInfo)) {
        throw new RuntimeException("Audio line not supported with the current format. Please check your audio hardware.")
      }
      
      AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
    }
    
    def openAndStartLine(line: TargetDataLine): Try[TargetDataLine] = Try {
      line.open(audioFormat)
      line.start()
      line
    }
    
    def executeRecording(line: TargetDataLine): Try[Unit] = Try {
      targetDataLine = Some(line)
      val inputStream = new AudioInputStream(line)
      audioInputStream = Some(inputStream)
      isRecording = true
      
      println("Recording started. Press Enter to stop recording...")
      
      // Start recording using Future
      val recordingTask = Future {
        Try {
          AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile))
          () // Return Unit explicitly
        }.recover {
          case e: IOException => 
            println(s"Error writing audio file: ${e.getMessage}")
            throw e
        }.get 
      }
      
      recordingFuture = Some(recordingTask)
      
      // Wait for user input to stop recording
      val scanner = new Scanner(System.in)
      scanner.nextLine()
      
      stopRecording()
      
      // Wait for the recording task to complete with a timeout
      Await.result(recordingTask, 1.seconds)
      
      val outputFileInfo = BFile(outputFile)
      val fileSizeMB = if (outputFileInfo.exists) {
        (outputFileInfo.size.toDouble / (1024 * 1024)).formatted("%.2f")
      } else "unknown"
      println(s"Recording completed and saved to: $outputFile (${fileSizeMB} MB)")
    }
    
    // Compose the operations using for comprehension
    val result = for {
      line <- setupAudioLine()
      openedLine <- openAndStartLine(line)
      _ <- executeRecording(openedLine)
    } yield ()
    
    result match {
      case Success(_) => Right(())
      case Failure(e: SecurityException) => 
        Left(s"Security error accessing audio system: ${e.getMessage}")
      case Failure(e: LineUnavailableException) => 
        Left(s"Audio line unavailable: ${e.getMessage}. The microphone might be in use by another application.")
      case Failure(e: java.util.concurrent.TimeoutException) => 
        println("Recording cleanup completed (with timeout)")
        Right(())
      case Failure(e: IOException) => 
        Left(s"Failed to write audio file: ${e.getMessage}")
      case Failure(e: RuntimeException) if e.getMessage.contains("Audio line not supported") => 
        Left(e.getMessage)
      case Failure(e) => 
        Left(s"Recording failed with error: ${e.getMessage}")
    }
  }
  
  def stopRecording(): Unit = {
    isRecording = false
    targetDataLine.foreach { line =>
      line.stop()
      line.close()
    }
    audioInputStream.foreach(_.close())
    targetDataLine = None
    audioInputStream = None
    recordingFuture = None
    println("Recording stopped.")
  }
  
  def listAvailableAudioDevices(): Unit = {
    println("Available audio input devices:")
    val mixerInfos = AudioSystem.getMixerInfo
    
    mixerInfos.zipWithIndex.foreach { case (mixerInfo, index) =>
      val mixer = AudioSystem.getMixer(mixerInfo)
      val targetLines = mixer.getTargetLineInfo
      
      if (targetLines.nonEmpty) {
        println(s"$index: ${mixerInfo.getName} - ${mixerInfo.getDescription}")
        targetLines.foreach { lineInfo =>
          println(s"    Line: ${lineInfo.getLineClass.getSimpleName}")
        }
      }
    }
  }

  def deleteRecordingFiles(): Either[String, Unit] = {
    val recordingsDir = BFile(".")
    val wavFiles = recordingsDir.glob("recording-*").toSeq
    
    if (wavFiles.isEmpty) {
      return Left("No recording files found to delete.")
    }
    
    val errors = wavFiles.flatMap { file =>
      Try {
        file.delete()
        println(s"Deleted recording file: ${file.name}")
      }.failed.toOption.map(e => s"Failed to delete file '${file.name}': ${e.getMessage}")
    }

    if (errors.nonEmpty) Left(errors.mkString("; "))
    else Right(())
  }
  def deleteRecordingFile(filename: String): Either[String, Unit] = {
    val file = BFile(filename)
    if (file.exists) {
      Try {
        file.delete()
      }.toEither.left.map(e => s"Failed to delete file '$filename': ${e.getMessage}")
        .map(_ => ())
        } else {
      Left(s"File '$filename' does not exist.")
        }
  }
  
  private def createSafeFilename(filename: String): String = {
    // Replace invalid characters for filenames
    filename.replaceAll("[<>:\"/\\\\|?*]", "-")
  }
  
  private def generateTimestampedFilename(extension: String): String = {
    val timestamp = java.time.LocalDateTime.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    s"recording-$timestamp.$extension"
  }
  

  
  
  def cleanup(): Unit = {
    if (isRecording) {
      println("Cleaning up recording resources...")
      stopRecording()
      
      // Give the Future a chance to complete gracefully
      recordingFuture.foreach { future =>
        try {
          Await.ready(future, 2.seconds)
        } catch {
          case _: java.util.concurrent.TimeoutException =>
            println("Recording Future didn't complete in time, proceeding with cleanup")
          case _: Exception =>
            // Future may have completed with an exception, that's okay
        }
      }
    }
    // No need to shutdown ExecutionContext.global - it's managed by the runtime
  }
  
  def main(args: Array[String]): Unit = {
    if (args.length > 0 && args(0) == "deleterecordings") {
      deleteRecordingFiles()
      cleanup()
      return
    }
    println("=== Scala CLI Audio Recorder ===")
    println()
    
    if (args.contains("--list-devices")) {
      listAvailableAudioDevices()
      cleanup()
      return
    }

    println("Commands:")
    println("  --list-devices  : List available audio input devices")
    println()

    val genFileName = generateTimestampedFilename("wav")
    startRecording(genFileName) match {
      case Right(_) => 
        println("Recording session completed successfully!")
      case Left(errorMessage:String) => 
        println(s"Recording failed: $errorMessage")
    }
    
    cleanup()
  }

// Run the main method

