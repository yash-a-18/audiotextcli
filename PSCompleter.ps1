# you can bring this into your environment by Dot loading
# or 
# . .\PSCompleter.ps1

$batfile = ".\target\pack\bin\audio-text-cli.bat"

Class WavFileName : System.Management.Automation.IValidateSetValuesGenerator {
    [string[]] GetValidValues () {
        $wavFiles = Get-ChildItem -Path ".\" -Filter "*.wav" | Select-Object -ExpandProperty Name
        $wavFiles = @('--listDevices') + $wavFiles
        return $wavFiles
    }
}    


function whisper {
    param (
        [Parameter(Mandatory)]
        [ValidateSet("about","whisper", "record","deleterecordings")]
        [string]
        $command,

        [Parameter(Mandatory = $false)]
        [ValidateSet([WavFileName])]
        [string]
        $wavFile 
    )

    if($command -eq "about") {
        & $batfile
    }
    elseif($command -eq "record") {
        # Handle special case for listing devices
        if ($wavFile -eq "--listDevices") {
            & $batfile record --listDevices
        } else 
        {
            & $batfile record
        }
    }
    elseif( $command -eq "whisper") {
        & $batfile whisper $wavFile
    } 
    elseif( $command -eq "deleterecordings") {
        # Handle deletion of .wav files
        & $batfile deleterecordings
    }
    else {
        Write-Error "Invalid command. Use 'whisper' or 'record' or 'deleterecordings'."
    }
}    