# simulate_idrive.ps1
# Helper script to simulate BMW iDrive inputs via ADB for testing.

param (
    [Parameter(Mandatory=$true)]
    [ValidateSet("zoom", "move", "click")]
    [string]$Action,

    [float]$Value1 = 0,
    [float]$Value2 = 0
)

function Send-Move {
    param($dx, $dy)
    Write-Host "Simulating Touchpad/Mouse Move: DX=$dx, DY=$dy"
    # Uses mouse source which triggers onGenericMotionEvent in MainActivity
    adb shell input mouse move $dx $dy
}

function Send-Zoom {
    param($delta)
    Write-Host "Simulating Rotary Scroll: Delta=$delta"
    # Note: 'input mouse scroll' usually sends AXIS_VSCROLL.
    # If the app specifically requires AXIS_SCROLL from SOURCE_ROTARY_ENCODER,
    # physical hardware or 'sendevent' may be required for 100% accuracy.
    # However, 'mouse scroll' is the closest standard ADB simulation.
    adb shell input mouse scroll $delta
}

function Send-Click {
    Write-Host "Simulating iDrive Center Click (KEYCODE_ENTER)"
    adb shell input keyevent 66
}

switch ($Action) {
    "move"  { Send-Move $Value1 $Value2 }
    "zoom"  { Send-Zoom $Value1 }
    "click" { Send-Click }
}
