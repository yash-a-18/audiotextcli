# you can bring this into your environment by Dot loading
# or 
# . .\PSCompleter.ps1


Class WavFileName : System.Management.Automation.IValidateSetValuesGenerator {
    [string[]] GetValidValues () {
        $wavFiles = Get-ChildItem -Path ".\" -Filter "*.wav" | Select-Object -ExpandProperty Name
        $wavFiles = @("--listDevices") + $wavFiles
        return $wavFiles
    }
}    


function whisper {
    param (
        [Parameter(Mandatory)]
        [ValidateSet("whisper", "record")]
        [string]
        $command,

        [Parameter(Mandatory = $false)]
        [ValidateSet([WavFileName])]
        [string]
        $wavFile 
    )

    if($command -eq "record") {
        # Handle special case for listing devices
        if ($wavFile -eq "--listDevices") {
            & .\target\pack\bin\zio-console.bat record --listDevices
        } else 
        {
            & .\target\pack\bin\zio-console.bat record
        }
    }
    elseif( $command -eq "whisper") {
        & .\target\pack\bin\zio-console.bat whisper $wavFile
    } else {
        Write-Error "Invalid command. Use 'whisper' or 'record'."
    }
}    