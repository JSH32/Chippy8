package com.github.riku32.chippy8.VM;

import lombok.Getter;
import lombok.Setter;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class Chip8 {
    // 16 8-bit registers (VX) where X is 0-F
    @Getter
    private final byte[] V = new byte[16];

    // 4 kilobyte memory
    @Getter
    private final short[] memory = new short[4096];

    // Stack 16 in size with 16-bit values
    @Getter
    private final short[] stack = new short[16];

    // Stack pointer
    @Getter
    private short sp;

    // Current index and program counter registers
    @Getter
    private short index;

    @Getter
    // Program counter, first 200 bits reserved
    private short pc = 0x200;

    @Getter
    private final byte[] videoMemory = new byte[64*32];

    private final Keypad keypad;

    private final Random random = new Random();

    // Two timers used internally by CHIP-8
    @Getter
    @Setter
    private byte delayTimer, soundTimer;

    public boolean drawFlag = true;

    /**
     * While debugger has a paused flag this is needed internally
     * To prevent cycles on other threads (which is usually ok) while changing internal contents during state/rom loads
     */
    private boolean paused = false;

    public Chip8(Keypad keypad) {
        this.keypad = keypad;
    }

    /**
     * Load a ROM from byte buffer
     *
     * @param rom byte buffer
     */
    public void loadRom(byte[] rom) {
        paused = true;

        // Reset values
        pc = 0x200;
        sp = 0;
        index = 0;
        delayTimer = soundTimer = 0;

        Arrays.fill(stack, (short) 0);
        Arrays.fill(V, (byte) 0);
        Arrays.fill(videoMemory, (byte) 0);
        System.arraycopy(Constants.FONT_SET, 0, memory, 0, Constants.FONT_SET.length);

        for (int i = 0; i < rom.length; i++)
            this.memory[i + 0x200] = (short) (rom[i] & 0xFF);

        paused = false;
    }

    /**
     * Save state as a byte buffer
     *
     * @return state
     */
    public byte[] saveState() throws IOException {
        paused = true;

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        packer.packShort(pc);
        packer.packShort(sp);
        packer.packShort(index);

        // Registers
        packer.packArrayHeader(V.length);
        for (byte val : V)
            packer.packByte(val);

        // Stack
        packer.packArrayHeader(stack.length);
        for (short val : stack)
            packer.packShort(val);

        // Memory
        packer.packArrayHeader(memory.length);
        for (short val : memory)
            packer.packShort(val);

        // VRAM
        packer.packArrayHeader(videoMemory.length);
        for (byte val : videoMemory)
            packer.packByte(val);

        packer.close();

        paused = false;

        return packer.toByteArray();
    }

    /**
     * Load a state from byte buffer
     *
     * @param state byte buffer
     */
    public void loadState(byte[] state) throws IOException {
        paused = true;

        MessageUnpacker unpack = MessagePack.newDefaultUnpacker(state);

        pc = unpack.unpackShort();
        sp = unpack.unpackShort();
        index = unpack.unpackShort();

        // Registers
        int lenRegister = unpack.unpackArrayHeader();
        for (int i = 0; i < lenRegister; i++)
            V[i] = unpack.unpackByte();

        // Stack
        int lenStack = unpack.unpackArrayHeader();
        for (int i = 0; i < lenStack; i++)
            stack[i] = unpack.unpackShort();

        // Memory
        int lenMemory = unpack.unpackArrayHeader();
        for (int i = 0; i < lenMemory; i++)
            memory[i] = unpack.unpackShort();

        // VRAM
        int lenVRAM = unpack.unpackArrayHeader();
        for (int i = 0; i < lenVRAM; i++)
            videoMemory[i] = unpack.unpackByte();

        unpack.close();

        paused = false;
    }

    /**
     * Execute one CPU cycle
     *
     */
    public void cycle() {
        // Do not run cycle if paused
        if (paused) return;

        switch (op() & 0xF000) {
            case 0x000:
                switch (op()) {
                    case 0x00E0:
                        op_00E0();
                        return;
                    case 0x00EE:
                        op_00EE();
                        return;
                }
            case 0x1000:
                op_1NNN();
                return;
            case 0x2000:
                op_2NNN();
                return;
            case 0x3000:
                op_3XKK();
                return;
            case 0x4000:
                op_4XKK();
                return;
            case 0x5000:
                op_5XY0();
                return;
            case 0x6000:
                op_6XKK();
                return;
            case 0x7000:
                op_7XKK();
                return;
            case 0x8000:
                switch (opN()) {
                    case 0x0000:
                        op_8XY0();
                        return;
                    case 0x0001:
                        op_8XY1();
                        return;
                    case 0x0002:
                        op_8XY2();
                        return;
                    case 0x0003:
                        op_8XY3();
                        return;
                    case 0x0004:
                        op_8XY4();
                        return;
                    case 0x0005:
                        op_8XY5();
                        return;
                    case 0x0006:
                        op_8XY6();
                        return;
                    case 0x0007:
                        op_8XY7();
                        return;
                    case 0x000E:
                        op_8XYE();
                        return;
                }
            case 0x9000:
                op_9XY0();
                return;
            case 0xA000:
                op_ANNN();
                return;
            case 0xB000:
                op_BNNN();
                return;
            case 0xC000:
                op_CXKK();
                return;
            case 0xD000:
                op_DXYN();
                return;
            case 0xE000:
                switch (opKK()) {
                    case (byte) 0x009E:
                        op_EX9E();
                        return;
                    case (byte) 0x00A1:
                        op_EXA1();
                        return;
                }
            case 0xF000:
                switch(opKK()) {
                    case 0x0007:
                        op_FX07();
                        return;
                    case 0x000A:
                        op_FX0A();
                        return;
                    case 0x0015:
                        op_FX15();
                        return;
                    case 0x0018:
                        op_FX18();
                        return;
                    case 0x001E:
                        op_FX1E();
                        return;
                    case 0x0029:
                        op_FX29();
                        return;
                    case 0x0033:
                        op_FX33();
                        return;
                    case 0x0055:
                        op_FX55();
                        return;
                    case 0x0065:
                        op_FX65();
                        return;
                }
            default:
                // Skip over invalid instruction (NOP)
                pc += 2;
        }
    }

    /**
     * Get string representation of opcode at program counter
     */
    public String disassembleOpcode(short pc) {
        switch (op(pc) & 0xF000) {
            case 0x000:
                switch (op(pc)) {
                    case 0x00E0: return "CLS";
                    case 0x00EE: return "RET";
                }
            case 0x1000: return String.format("JP %X", opNNN(pc));
            case 0x2000: return String.format("CALL %X", opNNN(pc));
            case 0x3000: return String.format("SE V%X, %X", opX(pc), opKK(pc));
            case 0x4000: return String.format("SNE V%X, %X", opX(pc), opKK(pc));
            case 0x5000: return String.format("SE V%X, V%X", opX(pc), opY(pc));
            case 0x6000: return String.format("LD V%X, %X", opX(pc), opKK(pc));
            case 0x7000: return String.format("ADD V%X, %X", opX(pc), opKK(pc));
            case 0x8000:
                switch (opN(pc)) {
                    case 0x0000: return String.format("LD V%X, V%X", opX(pc), opY(pc));
                    case 0x0001: return String.format("OR V%X, V%X", opX(pc), opY(pc));
                    case 0x0002: return String.format("AND V%X, V%X", opX(pc), opY(pc));
                    case 0x0003: return String.format("XOR V%X, V%X", opX(pc), opY(pc));
                    case 0x0004: return String.format("ADD V%X, V%X", opX(pc), opY(pc));
                    case 0x0005: return String.format("SUB V%X, V%X", opX(pc), opY(pc));
                    case 0x0006: return String.format("SHR V%X", opX(pc));
                    case 0x0007: return String.format("SUBN V%X, V%X", opX(pc), opY(pc));
                    case 0x000E: return String.format("SHL V%X", opX(pc));
                }
            case 0x9000: return String.format("SNE V%X, V%X", opX(pc), opY(pc));
            case 0xA000: return String.format("LD I, %X", opNNN(pc));
            case 0xB000: return "JP V0, nnn";
            case 0xC000: return String.format("RND V%X, %X", opX(pc), opKK(pc));
            case 0xD000: return String.format("DRW V%X, V%X, %X", opX(pc), opY(pc), opN(pc));
            case 0xE000:
                switch (opKK(pc)) {
                    case (byte) 0x009E: return String.format("SKP V%X", opX(pc));
                    case (byte) 0x00A1: return String.format("SKNP V%X", opX(pc));
                }
            case 0xF000:
                switch(opKK(pc)) {
                    case 0x0007: return String.format("LD V%X, DT", opX(pc));
                    case 0x000A: return String.format("LD V%X, K", opX(pc));
                    case 0x0015: return String.format("LD DT, V%X", opX(pc));
                    case 0x0018: return String.format("LD ST, V%X", opX(pc));
                    case 0x001E: return String.format("ADD I, V%X", opX(pc));
                    case 0x0029: return String.format("LD F, V%X", opX(pc));
                    case 0x0033: return String.format("LD B, V%X", opX(pc));
                    case 0x0055: return String.format("LD [I], V%X", opX(pc));
                    case 0x0065: return String.format("LD V%X, [I]", opX(pc));
                }
            default: return "NOP"; // No operation
        }
    }

    // Get opcode at program counter
    private short op() { return op(pc); }
    private short op(short pc) {
        if (pc < 0 || pc >= memory.length - 1)
            return 0;
        return (short) (memory[pc] << 8 | memory[pc + 1]);
    }

    // Lower nybble of high byte
    private byte opX() {
        return opX(pc);
    }
    private byte opX(short pc) {
        return (byte) (op(pc) >> 8 & 0x000F);
    }

    // Upper nybble of high byte
    private byte opY() {
        return opY(pc);
    }
    private byte opY(short pc) {
        return (byte) (op(pc) >> 4 & 0x000F);
    }

    // Lowest 4 bits
    private byte opN() {
        return opN(pc);
    }
    private byte opN(short pc) {
        return (byte) (op(pc) & 0x000F);
    }

    // Lowest 8 bits
    private byte opKK() {
        return opKK(pc);
    }
    private byte opKK(short pc) {
        return (byte) (op(pc) & 0x00FF);
    }

    // Lowest 12 bits
    private short opNNN() {
        return opNNN(pc);
    }
    private short opNNN(short pc) {
        return (short) (op(pc) & 0x0FFF);
    }

    // Put pixel on screen (with wrapping)
    private boolean setPixel(int x, int y) {
        if (x > 63) {
            x -= 63;
        } else if (x < 0) {
            x += 63;
        }

        if (y > 31) {
            y -= 31;
        } else if (y < 0) {
            y += 31;
        }

        int location = x + (y * 64);
        this.videoMemory[location] ^= 1;

        return this.videoMemory[location] != 1;
    }

    // CLS
    private void op_00E0() {
        // Clear video memory
        Arrays.fill(videoMemory, (byte) 0x0);

        drawFlag = true;
        pc += 2;
    }

    // RET
    private void op_00EE() {
        if (sp > 0) {
            --sp;
            pc = stack[sp];
        }

        pc += 2;
    }

    // JP addr
    private void op_1NNN() {
        pc = opNNN();
    }

    // CALL addr
    private void op_2NNN() {
        stack[sp++] = pc; // Store current pc at top of stack
        pc = opNNN(); // Set pc to opcode argument
    }

    // SE Vx, byte
    private void op_3XKK() {
        if (V[opX()] == opKK()) pc += 2;

        pc += 2;
    }

    // SNE Vx, byte
    private void op_4XKK() {
        if (V[opX()] != opKK()) pc += 2;

        pc += 2;
    }

    // SE Vx, Vy
    private void op_5XY0() {
        if (V[opX()] == V[opY()]) pc += 2;

        pc += 2;
    }

    // LD Vx, byte
    private void op_6XKK() {
        V[opX()] = opKK();

        pc += 2;
    }

    // ADD Vx, byte
    private void op_7XKK() {
        V[opX()] += opKK();

        pc += 2;
    }

    // LD Vx, Vy
    private void op_8XY0() {
        V[opX()] = V[opY()];

        pc += 2;
    }

    // OR Vx, Vy
    private void op_8XY1() {
        V[opX()] |= V[opY()];

        pc += 2;
    }

    // AND Vx, Vy
    private void op_8XY2() {
        V[opX()] &= V[opY()];

        pc += 2;
    }

    // XOR Vx, Vy
    private void op_8XY3() {
        V[opX()] ^= V[opY()];

        pc += 2;
    }

    // ADD Vx, Vy
    private void op_8XY4() {
        // VF is set to 1 if the result was greater than 255, otherwise 0
        byte sum = (byte) (V[opX()] + V[opY()]);
        V[15] = (byte) (((sum & 0xff) < (V[opY()] & 0xff) || (sum & 0xff) < (V[opX()] & 0xff)) ? 1 : 0);
        V[opX()] = sum;

        pc += 2;
    }

    // SUB Vx, Vy
    private void op_8XY5() {
        // VF is set to 1 if Vx > Vy, otherwise 0
        V[15] = (byte) ((V[opY()] > V[opX()]) ? 0 : 1);

        // Subtract
        V[opX()] -= V[opY()];

        pc += 2;
    }

    // SHR Vx
    private void op_8XY6() {
        V[15] = (byte) (V[opX()] & 1);
        V[opX()] = (byte) ((V[opX()] & 0xFF) >>> 1);

        pc += 2;
    }

    // SUBN Vx, Vy
    private void op_8XY7() {
        V[15] = (byte) ((V[opX()] > V[opY()]) ? 0 : 1);
        V[opX()] = (byte) (V[opY()] - V[opX()]);

        pc += 2;
    }

    // SHL Vx
    private void op_8XYE() {
        V[15] = (byte) (((V[opX()] & 0x80) != 0) ? 1 : 0);
        V[opX()] = (byte) ((V[opX()] & 0xFF) << 1);

        pc += 2;
    }

    // SNE Vx, Vy
    private void op_9XY0() {
        if (V[opX()] != V[opY()]) pc += 2;

        pc += 2;
    }

    // LD I, addr
    private void op_ANNN() {
        index = opNNN();

        pc += 2;
    }

    // JP V0, addr
    private void op_BNNN() {
        pc = (short) (V[0] & 0xff + opNNN() & 0xfff);
    }

    // RND Vx, byte
    private void op_CXKK() {
        V[opX()] = (byte) (random.nextInt(256) & opKK());

        pc += 2;
    }

    // DRW Vx, Vy, nibble
    private void op_DXYN() {
        this.V[15] = 0;
        for (int y = 0; y < opN(); y++) {
            int pixel = this.memory[this.index + y];
            for (int x = 0; x < 8; x++) {
                if ((pixel & 0x80) > 0) {
                    if (setPixel(V[opX()] + x, V[opY()] + y)) {
                        V[15] = 1;
                    }
                }

                pixel <<= 1;
            }
        }

        drawFlag = true;
        pc += 2;
    }

    // SKP Vx
    private void op_EX9E() {
        if (keypad.pressed(V[opX()] & 0x0F)) pc += 2;

        pc += 2;
    }

    // SKNP Vx
    private void op_EXA1() {
        if (!keypad.pressed(V[opX()] & 0x0F)) pc += 2;

        pc += 2;
    }

    // LD Vx, DT
    private void op_FX07() {
        V[opX()] = delayTimer;

        pc += 2;
    }

    // LD Vx, K
    private void op_FX0A() {
        boolean pressed = false;
        for (int i = 0; i < 16; i++) {
            if (keypad.pressed(i)) {
                V[opX()] = (byte) i;
                pressed = true;
            }
        }

        // If no key was pressed wait for a key press, do not continue
        if (pressed)
            pc += 2;
    }

    // LD DT, Vx
    private void op_FX15() {
        delayTimer = V[opX()];

        pc += 2;
    }

    // LD ST, Vx
    private void op_FX18() {
        soundTimer = V[opX()];

        pc += 2;
    }

    // ADD I, Vx
    private void op_FX1E() {
        V[15] = (byte) (V[opX()] & 0xFF + index & 0xFFFF);
        index += V[opX()];

        pc += 2;
    }

    // LD F, Vx
    private void op_FX29() {
        index = (short) (V[opX()] * 5);

        pc += 2;
    }

    // LD B, Vx
    private void op_FX33() {
        int uVX = V[opX()] & 0xff;

        memory[index] = (short) ((uVX % 1000) / 100);
        memory[index+1] = (short) ((uVX % 100) / 10);
        memory[index+2] = (short) (uVX % 10);

        pc += 2;
    }

    // LD [I], Vx
    private void op_FX55() {
        for (int i = 0; i <= opX(); i++)
            memory[index+i] = V[i];

        pc += 2;
    }

    // LD Vx, [I]
    private void op_FX65() {
        for (int i = 0; i <= opX(); i++)
            V[i] = (byte) memory[index+i];

        pc += 2;
    }
}
