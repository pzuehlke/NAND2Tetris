// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Fill.asm

// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, 
// the screen should be cleared.


// Pseudo-code (à la Python):
    //     pos=0
    // (LOOP)
    //     key = read KBD
    //     if KBD > 0:
    //     (BLACKEN)
    //         *(SCREEN + pos) = 255 (-1)
    //         pos += 1
    //         if pos > maxpos:
    //             pos = 0
    //         goto LOOP
    //     else:
    //     (CLEAR)
    //         *(SCREEN + pos) = 0
    //         pos -= 1
    //         if pos < 0:
    //             pos = maxpos
    //         goto LOOP

    @8192   // Number of 16-bit words in the screen memory map
    D=A
    @maxpos
    M=D
    @pos
    M=0

(LOOP)
    @KBD
    D=M
    @BLACKEN
    D;JNE
    @CLEAR
    D;JEQ
    @LOOP
    0;JMP

(BLACKEN)
    @SCREEN
    D=A
    @pos
    A=D+M
    M=-1
    @pos
    M=M+1
    @pos
    D=M
    @maxpos
    D=D-M
    @MAX_POS
    D;JGT
    @LOOP
    0;JMP

(CLEAR)
    @SCREEN
    D=A
    @pos
    A=D+M
    M=0
    @pos
    M=M-1
    @pos
    D=M
    @NEGATIVE
    D;JLT
    @LOOP
    0;JMP

(MAX_POS)
    @pos
    M=0
    @LOOP
    0;JMP

(NEGATIVE)
    @maxpos
    D=M
    @pos
    M=D
    @LOOP
    0;JMP
