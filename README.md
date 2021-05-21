<p align="center">
  <img src="https://raw.githubusercontent.com/Riku32/Chippy8/master/.git/branding/banner.png">
</p>
CHIP-8 emulator implementation written in Java for APCS. This emulator has a debugger built in and runs at 600hz.

### What is CHIP-8?
CHIP-8 is a virtual machine, developed by Joseph Weisbecker. It was initially used on the COSMAC VIP and Telmac 1800 8-bit microcomputers in the mid-1970s. CHIP-8 programs are run on a CHIP-8 virtual machine. It was made to allow video games to be more easily programmed for these computers.

### Controls
Each game has its own set of unique controls. It should be easy to figure them out though. The original CHIP-8 keys have been mapped to new keys on your keyboard. Below are the original and mapped controls

#### Original
| | | | |
|-|-|-|-|
|1|2|3|C|
|4|5|6|D|
|7|8|9|E|
|A|0|B|F|

#### Mapped
| | | | |
|-|-|-|-|
|1|2|3|4|
|Q|W|E|R|
|A|S|D|F|
|Z|X|C|V|

### Sources
- [Cowgod's Chip-8 Technical Reference](http://devernay.free.fr/hacks/chip8/C8TECH10.HTM)
- [CHIP-8 Wiki](https://github.com/mattmikolay/chip-8/wiki/Mastering-CHIP%E2%80%908)
- [Chip-8 Test Rom](https://github.com/corax89/chip8-test-rom)
- [Chip 8 Instruction Scheduling and Frequency](https://jackson-s.me/2019/07/13/Chip-8-Instruction-Scheduling-and-Frequency.html)

### Notes
* Flickering is intentional, due to draw/clear calls being separate and sometimes many opcodes apart there is flickering. Due to this same reason double buffering is not possible, thus the screen flickers.
* CHIP-8 implementations are all over the place. Each emulator has its own slightly different way of doing things. I have followed the spec on Cowgod's reference, but not all games will run on the emulator properly. The `Roms` subdirectory contains some simple programs I have tested that work with the emulator.

### Libraries
Some libraries were used in the making of this project, some are essential and others are just there for cleanliness. I will list all libraries and their functions below.
|Library|Function|
|-|-|
|[jinput](https://jinput.github.io/jinput/)|Capturing user keyboard input|
|[Lombok](https://projectlombok.org/)|Replacing getters/setters with annotations, lowers LoC significantly|
|[Flatlaf](https://www.formdev.com/flatlaf/)|Look and Feel for Java GUI, makes things not ugly|
|[Msgpack](https://msgpack.org/)|Fast file serialization|

### How to compile?
Due to using dependencies we require the maven build system, please make sure you have it installed first

1. Open terminal in the project directory
2. Run `mvn assembly:single`

This should have created a target folder with a jar artifact.