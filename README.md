# FLRPC

Discord Rich Presence for FL Studio (current only Windows)

## Supported FL Versions

|  FL Version   |  FLRPC Version   |
|:-------------:|:----------------:|
| `21.1.0.3713` |     `0.2.1`      |
| `21.0.3.3517` | `0.1.x`, `0.2.0` |

## Installation

1. Download and unzip the [latest release](https://github.com/Gluton-Official/FLRPC/releases/latest)
2. In FL Studio, go to Options > File Settings.
   Under External Tools, add the path to the `FLRPC.exe` and select Launch at Startup

   ![image](https://github.com/Gluton-Official/FLRPC/assets/66543311/9a588342-25d8-43fc-84e0-a88272a55719)


When you launch FL Studio and Discord is open, your activity should be Playing FL Studio,
with the project file name and time spent working displayed beneath.

![image](https://github.com/Gluton-Official/FLRPC/assets/66543311/e2eee109-4901-4874-a7a6-497c8482cba2)

> If you have manually added FL Studio to your registered games in Discord's Activity Settings, you may have to remove it for FLRPC to display rich presence information.

> You can also run FLRPC manually through FL Studio under Tools > FLRPC, or by running the exe directly.
> 
> To close FLRPC manually, click the FLRPC icon in your system tray and select Quit.

> Due to the way FL Studio loads project files,
> the time spent working may not be accurate if the project file was opened directly.
> Opening the Project info (F11) should correct the displayed time.
